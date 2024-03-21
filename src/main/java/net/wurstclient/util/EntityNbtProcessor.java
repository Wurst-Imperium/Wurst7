package net.wurstclient.util;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class EntityNbtProcessor
{
	public enum NbtType
	{
		COMPOUND,
		LIST,
		STRING;
		// more
	}
	
	static class Storage
	{
		public NbtCompound compound = null;
		
		public NbtList list = null;
		NbtType _listContentType = null;
		
		public NbtType getListContentType()
		{
			return _listContentType;
		}
		
		public void setListContentType(NbtType type)
		{
			_listContentType = type;
		}
		
		public String string = null;
	}
	
	static class InvalidNbtStateError extends Error
	{
		InvalidNbtStateError(String message)
		{
			super(message);
		}
	}
	
	Storage storage = new Storage();
	
	private NbtCompound baseCompound = new NbtCompound();
	
	public NbtCompound getBaseCompound()
	{
		return baseCompound;
	}
	
	public void setCurrentCompoundAsBase()
	{
		baseCompound = storage.compound.copy();
	}
	
	NbtType currentState = NbtType.COMPOUND;
	
	public EntityNbtProcessor(Entity entity)
	{
		baseCompound = entity.writeNbt(baseCompound);
		storage.compound = baseCompound.copy();
	}
	
	// get and store value
	public EntityNbtProcessor compound(String key)
	{
		if(currentState != NbtType.COMPOUND)
			throw new InvalidNbtStateError("Not compound");
		
		storage.compound = storage.compound.getCompound(key);
		
		return this;
	}
	
	public EntityNbtProcessor string(String key)
	{
		if(currentState != NbtType.COMPOUND)
			throw new InvalidNbtStateError("Not compound");
		
		currentState = NbtType.STRING;
		storage.string = storage.compound.getString(key);
		
		return this;
	}
	// more
	
	private int _mapNbtTypeToInt(NbtType type)
	{
		// According to
		// https://bukkit.org/threads/nbttagcompound-getlist-tileentities-and-nbttagcompound-getlist-items-8.210548/
		// 0:"END", 1:"BYTE", 2:"SHORT", 3:"INT", 4:"LONG",
		// 5:"FLOAT", 6:"DOUBLE", 7:"BYTE[]", 8:"STRING", 9:"LIST",
		// 10:"COMPOUND", 11:"INT[]"
		
		switch(type)
		{
			case COMPOUND ->
			{
				return 10;
			}
			case STRING ->
			{
				return 8;
			}
		}
		
		return -1;
	}
	
	public EntityNbtProcessor list(String key, NbtType type)
	{
		if(currentState != NbtType.COMPOUND)
			throw new InvalidNbtStateError("Not compound");
		
		currentState = NbtType.LIST;
		storage.setListContentType(type);
		storage.list = storage.compound.getList(key, _mapNbtTypeToInt(type));
		
		return this;
	}
	
	public EntityNbtProcessor at(int index)
	{
		if(currentState != NbtType.LIST)
			throw new InvalidNbtStateError("Not list");
		
		switch(storage.getListContentType())
		{
			case COMPOUND ->
			{
				storage.compound = storage.list.getCompound(index);
			}
			// more
		}
		
		currentState = storage.getListContentType();
		
		return this;
	}
	
	// return value
	public Object value()
	{
		switch(currentState)
		{
			case COMPOUND ->
			{
				return storage.compound;
			}
			case LIST ->
			{
				return storage.list;
			}
			case STRING ->
			{
				return storage.string;
			}
			// more
		}
		
		return null;
	}
	
	// auto resolve
	// NOTE: does not support lists!
	@Deprecated
	public Object resolve(String chainedKeys)
	{
		String[] compoundKeys = chainedKeys.split("\\.");
		
		for(int i = 0, mi = compoundKeys.length - 1; i < mi; ++i)
		{
			compound(compoundKeys[i]);
		}
		
		return storage.compound.get(compoundKeys[compoundKeys.length - 1]);
	}
}
