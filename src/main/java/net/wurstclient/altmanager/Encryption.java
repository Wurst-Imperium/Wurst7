/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;
import net.wurstclient.util.json.WsonObject;

public final class Encryption
{
	private static final String CHARSET = "UTF-8";
	
	private final Cipher encryptCipher;
	private final Cipher decryptCipher;
	
	public Encryption(Path encFolder) throws IOException
	{
		createEncryptionFolder(encFolder);
		
		KeyPair rsaKeyPair =
			getRsaKeyPair(encFolder.resolve("wurst_rsa_public.txt"),
				encFolder.resolve("wurst_rsa_private.txt"));
		
		SecretKey aesKey =
			getAesKey(encFolder.resolve("wurst_aes.txt"), rsaKeyPair);
		
		try
		{
			encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
			encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey,
				new IvParameterSpec(aesKey.getEncoded()));
			
			decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
			decryptCipher.init(Cipher.DECRYPT_MODE, aesKey,
				new IvParameterSpec(aesKey.getEncoded()));
			
		}catch(GeneralSecurityException e)
		{
			throw new CrashException(
				CrashReport.create(e, "Creating AES ciphers"));
		}
	}
	
	private Path createEncryptionFolder(Path encFolder) throws IOException
	{
		Files.createDirectories(encFolder);
		if(Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS)
			Files.setAttribute(encFolder, "dos:hidden", true);
		
		Path readme = encFolder.resolve("READ ME I AM VERY IMPORTANT.txt");
		String readmeText = "DO NOT SHARE THESE FILES WITH ANYONE!\r\n"
			+ "They are encryption keys that protect your alt list file from being read by someone else.\r\n"
			+ "If someone is asking you to send these files, they are 100% trying to scam you.\r\n"
			+ "\r\n"
			+ "DO NOT EDIT, RENAME OR DELETE THESE FILES! (unless you know what you're doing)\r\n"
			+ "If you do, Wurst's Alt Manager can no longer read your alt list and will replace it with a blank one.\r\n"
			+ "In other words, YOUR ALT LIST WILL BE DELETED.";
		Files.write(readme, readmeText.getBytes("UTF-8"),
			StandardOpenOption.CREATE);
		
		return encFolder;
	}
	
	public static Path chooseEncryptionFolder()
	{
		String userHome = System.getProperty("user.home");
		String xdgDataHome = System.getenv("XDG_DATA_HOME");
		String encFolderName = ".Wurst encryption";
		
		Path homeEncFolder = Paths.get(userHome, encFolderName).normalize();
		Path encFolder = homeEncFolder;
		if(xdgDataHome != null && !xdgDataHome.isEmpty())
		{
			encFolder = Paths.get(xdgDataHome, encFolderName).normalize();
			
			if(!Files.exists(encFolder) && Files.isDirectory(homeEncFolder))
				migrateEncryptionFolder(homeEncFolder, encFolder);
		}
		
		return encFolder;
	}
	
	public static void migrateEncryptionFolder(Path oldFolder, Path newFolder)
	{
		System.out.println("Migrating encryption folder from " + oldFolder
			+ " to " + newFolder);
		
		try
		{
			Files.createDirectories(newFolder);
			
			File[] oldFiles = oldFolder.toFile().listFiles();
			for(File oldFile : oldFiles)
			{
				Path fileDestination = newFolder.resolve(oldFile.getName());
				Files.copy(oldFile.toPath(), fileDestination);
			}
			
			for(File oldFile : oldFiles)
				oldFile.delete();
			
			Files.deleteIfExists(oldFolder);
			
		}catch(IOException e)
		{
			CrashReport report =
				CrashReport.create(e, "Migrating Wurst encryption folder");
			CrashReportSection section = report.addElement("Migration");
			section.add("Old path", oldFolder);
			section.add("New path", newFolder);
			throw new CrashException(report);
		}
	}
	
	public byte[] decrypt(byte[] bytes)
	{
		try
		{
			return decryptCipher.doFinal(Base64.getDecoder().decode(bytes));
			
		}catch(IllegalArgumentException | GeneralSecurityException e)
		{
			throw new CrashException(CrashReport.create(e, "Decrypting bytes"));
		}
	}
	
	public String loadEncryptedFile(Path path) throws IOException
	{
		try
		{
			return new String(decrypt(Files.readAllBytes(path)), CHARSET);
			
		}catch(CrashException e)
		{
			throw new IOException(e);
		}
	}
	
	public JsonElement parseFile(Path path) throws IOException, JsonException
	{
		try(BufferedReader reader = Files.newBufferedReader(path))
		{
			return JsonParser.parseString(loadEncryptedFile(path));
			
		}catch(JsonParseException e)
		{
			throw new JsonException(e);
		}
	}
	
	public WsonArray parseFileToArray(Path path)
		throws IOException, JsonException
	{
		JsonElement json = parseFile(path);
		
		if(!json.isJsonArray())
			throw new JsonException();
		
		return new WsonArray(json.getAsJsonArray());
	}
	
	public WsonObject parseFileToObject(Path path)
		throws IOException, JsonException
	{
		JsonElement json = parseFile(path);
		
		if(!json.isJsonObject())
			throw new JsonException();
		
		return new WsonObject(json.getAsJsonObject());
	}
	
	public byte[] encrypt(byte[] bytes)
	{
		try
		{
			return Base64.getEncoder().encode(encryptCipher.doFinal(bytes));
			
		}catch(GeneralSecurityException e)
		{
			throw new CrashException(CrashReport.create(e, "Encrypting bytes"));
		}
	}
	
	public void saveEncryptedFile(Path path, String content) throws IOException
	{
		try
		{
			Files.write(path, encrypt(content.getBytes(CHARSET)));
			
		}catch(CrashException e)
		{
			throw new IOException(e);
		}
	}
	
	public void toEncryptedJson(JsonObject json, Path path)
		throws IOException, JsonException
	{
		try
		{
			saveEncryptedFile(path, JsonUtils.PRETTY_GSON.toJson(json));
			
		}catch(JsonParseException e)
		{
			throw new JsonException(e);
		}
	}
	
	private KeyPair getRsaKeyPair(Path publicFile, Path privateFile)
		throws IOException
	{
		if(Files.notExists(publicFile) || Files.notExists(privateFile))
			return createRsaKeys(publicFile, privateFile);
		
		try
		{
			return loadRsaKeys(publicFile, privateFile);
			
		}catch(GeneralSecurityException | ReflectiveOperationException
			| IOException e)
		{
			System.err.println("Couldn't load RSA keypair!");
			e.printStackTrace();
			
			return createRsaKeys(publicFile, privateFile);
		}
	}
	
	private SecretKey getAesKey(Path path, KeyPair pair) throws IOException
	{
		if(Files.notExists(path))
			return createAesKey(path, pair);
		
		try
		{
			return loadAesKey(path, pair);
			
		}catch(GeneralSecurityException | IOException e)
		{
			System.err.println("Couldn't load AES key!");
			e.printStackTrace();
			
			return createAesKey(path, pair);
		}
	}
	
	private KeyPair createRsaKeys(Path publicFile, Path privateFile)
		throws IOException
	{
		try
		{
			System.out.println("Generating RSA keypair.");
			
			// generate keypair
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(1024);
			KeyPair pair = generator.generateKeyPair();
			
			KeyFactory factory = KeyFactory.getInstance("RSA");
			
			// save public key
			try(ObjectOutputStream out =
				new ObjectOutputStream(Files.newOutputStream(publicFile)))
			{
				RSAPublicKeySpec keySpec = factory.getKeySpec(pair.getPublic(),
					RSAPublicKeySpec.class);
				
				out.writeObject(keySpec.getModulus());
				out.writeObject(keySpec.getPublicExponent());
			}
			
			// save private key
			try(ObjectOutputStream out =
				new ObjectOutputStream(Files.newOutputStream(privateFile)))
			{
				RSAPrivateKeySpec keySpec = factory
					.getKeySpec(pair.getPrivate(), RSAPrivateKeySpec.class);
				
				out.writeObject(keySpec.getModulus());
				out.writeObject(keySpec.getPrivateExponent());
			}
			
			return pair;
			
		}catch(GeneralSecurityException e)
		{
			throw new CrashException(
				CrashReport.create(e, "Creating RSA keypair"));
		}
	}
	
	private SecretKey createAesKey(Path path, KeyPair pair) throws IOException
	{
		try
		{
			System.out.println("Generating AES key.");
			
			// generate key
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			SecretKey key = keygen.generateKey();
			
			// save key
			Cipher rsaCipher = Cipher.getInstance("RSA");
			rsaCipher.init(Cipher.ENCRYPT_MODE, pair.getPublic());
			Files.write(path, rsaCipher.doFinal(key.getEncoded()));
			
			return key;
			
		}catch(GeneralSecurityException e)
		{
			throw new CrashException(CrashReport.create(e, "Creating AES key"));
		}
	}
	
	private KeyPair loadRsaKeys(Path publicFile, Path privateFile)
		throws GeneralSecurityException, ReflectiveOperationException,
		IOException
	{
		KeyFactory factory = KeyFactory.getInstance("RSA");
		
		// load public key
		PublicKey publicKey;
		try(ObjectInputStream in =
			new ObjectInputStream(Files.newInputStream(publicFile)))
		{
			publicKey = factory.generatePublic(new RSAPublicKeySpec(
				(BigInteger)in.readObject(), (BigInteger)in.readObject()));
		}
		
		// load private key
		PrivateKey privateKey;
		try(ObjectInputStream in =
			new ObjectInputStream(Files.newInputStream(privateFile)))
		{
			privateKey = factory.generatePrivate(new RSAPrivateKeySpec(
				(BigInteger)in.readObject(), (BigInteger)in.readObject()));
		}
		
		return new KeyPair(publicKey, privateKey);
	}
	
	private SecretKey loadAesKey(Path path, KeyPair pair)
		throws GeneralSecurityException, IOException
	{
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
		
		return new SecretKeySpec(cipher.doFinal(Files.readAllBytes(path)),
			"AES");
	}
}
