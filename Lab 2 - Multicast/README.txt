# 18-842 Distributed Systems
# Spring 2016
# Lab 2: Multicast
# README.txt
# Daniel Santoro // ddsantor. Akansha Patel // akanshap.

COMPILATION:
If running in a Unix type command line environment, there is a small bash script
called "compileAndRun" in the Lab1-Clocks directory. This will compile the
project and start running it. Additionaly, in the same directory there is a 
"run" script. This will just run the program without compiling. If running multiple
nodes from one file system "compileAndRun" may be used to instantiate the first
node, but "run" must be used to instantiate the remaining nodes.

CONFIGURATION:
The configuration file (config.yaml) is also placed in the Group21-Lab2 directory.
When interacting with the user interface you'll find that the program requests the
name of the configuration file. A path can be accepted here so the configuration
file can actually be placed anywhere as long as there is a file path to it.

DEMO:
Per the configuration discussion above, it is our intention to demo using 
multiple CMU machines. That is, we intend to ssh into multiple machines on the 
Andrew File System to demo this project. In doing this, we can place the config.yaml
file in any Public folder on AFS so that all instances of MessagePasser can 
read from the same file.


