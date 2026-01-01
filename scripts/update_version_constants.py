import argparse
import re


def update_gradle_properties(mc_version, fabric_loader, fapi_version):
	print("Updating gradle.properties...")

	# Read gradle.properties
	with open("gradle.properties", "r") as f:
		lines = f.readlines()

	# Define replacements
	replacements = {
		"minecraft_version": lambda v: mc_version,
		"loader_version": lambda v: fabric_loader,
		"fabric_api_version": lambda v: fapi_version,
		"mod_version": lambda v: v[: v.index("MC") + 2] + mc_version,
	}

	# Update lines
	for i, line in enumerate(lines):
		if line.startswith("#"):
			continue
		parts = line.split("=")
		if len(parts) != 2:
			continue
		key = parts[0]
		if key.strip() not in replacements:
			continue
		old_value = parts[1]
		new_value = replacements[key.strip()](old_value)
		print(f"{key}={old_value} -> {new_value}")
		lines[i] = f"{key}={new_value}\n"

	# Save modified gradle.properties
	with open("gradle.properties", "w") as f:
		f.writelines(lines)
	print("gradle.properties updated.")


def update_mc_version_constant(mc_version):
	print(f"Updating MC_VERSION constant to {mc_version}...")

	# Read WurstClient.java
	with open("src/main/java/net/wurstclient/WurstClient.java", "r") as f:
		lines = f.readlines()

	# Update lines
	pattern = re.compile(r"(public static final String MC_VERSION = \")([^\"]+)(\";)")
	found = False
	for i, line in enumerate(lines):
		match = pattern.search(line)
		if match:
			lines[i] = pattern.sub(r"\g<1>" + mc_version + r"\g<3>", line)
			found = True
			break

	# Save modified WurstClient.java
	with open("src/main/java/net/wurstclient/WurstClient.java", "w", newline='\r\n') as f:
		f.writelines(lines)

	if found:
		print("MC_VERSION constant updated.")
	else:
		print("Couldn't find MC_VERSION constant in WurstClient.java.")
		exit(1)


if __name__ == "__main__":
	parser = argparse.ArgumentParser()
	parser.add_argument("mc_version", help="Minecraft version")
	parser.add_argument("fabric_loader", help="Fabric Loader version")
	parser.add_argument("fapi_version", help="Fabric API version")
	args = parser.parse_args()

	update_gradle_properties(
		args.mc_version,
		args.fabric_loader,
		args.fapi_version,
	)

	update_mc_version_constant(args.mc_version)
