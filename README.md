# UnstructuredP2P

Package contains two files:

peerList.java
unstructuredPeer.java

unstructuredPeer does the message request part of the node and the peerList does the request handling part
of the unstructured peer to peer network. This package needs apache package for Zipf's distribution to run
queries. The command format for compiling and running the code are as follows:

javac -classpath commons-math3-3.6.1 peerListen.java unstructuredPeer.java
java -classpath .:commons-math3-3.6.1 unstructurePeer <node port> <Bootstrap server IP> <Booststrap Server port>

The node registers to bootstrap with the given username and hop count and then starts the listening thread.
then the node enters into User Interface mode where it takes the following commands:

Usage:
	add <Resource name>:					 Adds resource to the node.
	remove <Resource name>:					 Deletes resource from the node.
	leave:									 Leaves the network.
	DEL UNAME <username>:					 Deletes the network <username> from Bootstrap Server.
	print routing:							 Prints routing table.
	print routing table size:				 Prints the size of routing table
	print resources:						 Prints resources in this node.
	answered:								 Gives the queries answered till now.
	forwarded:								 Gives the number of queries forwarded till now.
	distribute <resources per node>:		 Distributes the resources.txt contents to all the nodes in the network.
	query <(part of)file name>: 			 Queries the given file name or part of the file name
	queries <no of qeries> <zipfs exponent>: Generates the number of queries given wit hthat Zipf's exponent.
	exit: 									 Exits the program.