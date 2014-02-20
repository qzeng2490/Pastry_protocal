
1. Group Member: Qiang Zeng(UFID:29884423) 

2. Working:
          First, We generate the nodeId randomly. The base of the nodeId is determined by b and the length of the nodeId is determined by both numofnodes and b. Then we initiate the pastry Network by initiate every node's leafset and routing table.  Then send each node a message that ask each node to send a number of requests. Each node route a message which keep track of hops and hopLists. Forward function is the most important function that help to find the next hop. The Forward function first check the key whether it is within the range of current node's leafset, then select the node which is most close to key. If the key is not within range of leafset, we use the routing table. Find the corresponding row and col , if the entry is not "ffffffff" then forward to that node. The rare case is to check all the leafset and routing table. 
          We have successfully tested the network for 10000 Nodes and 5 requests after which the algorithm takes a large amount of time to give results.

3. largest network 10000 nodes 5 requests.

Execution : 
example :
	>scalac Project3.scala
	>scala Project3 3000 30
OUTPUT :
                    ...
Race Detected:	List(132132, 132120, 132011)
Race Detected:	List(023330, 023332, 023210, 022222, 032102, 132011)
Race Detected:	List(012230, 012313, 032102, 332020)
Race Detected:	List(101221, 101302, 102031, 123233, 332020)
Average hop per Message  =3.853191134810865
example :

	>scalac Project3.scala
	>scala Project3 10000 5
OUTPUT :
Race Detected:	List(1031321, 1031301, 1031001, 1032223, 1301101)
Race Detected:	List(2013211, 2013230, 2013011, 2012320, 2121001, 1301101)
Race Detected:	List(3110232, 3110220, 3110013, 3113222)
Race Detected:	List(1212001, 1212000, 1211010, 1232333, 1032223, 3113222)
Average hop per Message  =4.53475
      
