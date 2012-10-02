# PortableHole

PortableHole is a Bukkit plugin which lets you create temporary tunnels in the world.  Tunnels are opened with a special
written book, with the (configurable) title "Portable Hole", and automatically close after a time dependent on the tunnel
length.  The plugin respects world-protection plugins by firing and listening for a BlockBreakEvent.  Full control is 
provided over who can create tunnels, and a wide range of tunnelling costs (items, XP, hunger, health, money, item durability...)
is supported.

## Installation

Detailed documentation can be found on BukkitDev: http://dev.bukkit.org/server-mods/portablehole

## Building

To build PortableHole from source, you will need Maven.

1a) Download a copy of Vault.jar (get the latest version) from http://dev.bukkit.org/server-mods/vault/                                                        
                                                                                                                                                               
1b) Run 'mvn install:install-file -DgroupId=net.milkbowl.vault -DartifactId=Vault -Dversion=X.Y.Z -Dpackaging=jar -Dfile=Vault.jar' (adjust the version number to match what you downloaded)                                                                                                                                  

2a) Get a copy of dhutils: "git clone https://github.com/desht/dhutils.git"

2b) Build dhutils.  In the dhutils top-level directory, type: "mvn clean install"

3a) Download PortableHole: "git clone https://github.com/desht/portablehole.git"

3b) Build PortableHole.  In the top-level directory, type "mvn clean install"

This will give you a copy of PortableHole.jar in the target/ directory.

Use "mvn eclipse:eclipse" to create the .project and .classpath files if you want to open the project in Eclipse.

## Usage

Detailed documentation is available on bukkitdev: http://dev.bukkit.org/server-mods/portablehole/

## License

PortableHole by Des Herriott is licensed under the [Gnu GPL v3](http://www.gnu.org/licenses/gpl-3.0.html). 

It includes the third-party BookItem class by Joshua Reetz, also licensed under the Gnu GPL v3.

