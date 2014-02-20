//package Project3

/*project3*/
import actors.Actor
import scala.math._
import collection.immutable._
import scala.Array
import scala.Array._
import scala.util.Random

case class pastryinit( numRequests:Int)
case class parameter(row:Int,col:Int)
case class Nodeinit(mapIdnode:TreeMap[String, PastryNode])
case class MakeRequests(numRequests:Int)
case class DoRouting(message: Message,destNodeId: String)
case class RemindMe(requestsMade: Int)
case class Deliver(message: Message)
case class MessageRecieved(hops: Int)
case object ThresholdReached

class Message(Key: String){
  var hop: Int = 0
  var hopNodes:List[String] = Nil
}

object Project3{
  def main(args : Array[String]) {
    var b =2 
    var L =16
    //var numNodes = 10000 
    //var numRequests = 1
    var numNodes = args(0).toInt
    var numRequests = args(1).toInt
    var ROW = ceil(log(numNodes) / log(pow(2,b))).toInt
    var COL = pow(2,b).toInt
    println("row: "+ROW+"COL: "+ COL)
    NetWork.start()
    NetWork ! parameter(ROW,COL)
    for (i <- 0 to numNodes){
       NetWork.join(new PastryNode(numNodes))
      // println(i+":  "+NetWork.nodeIds)     
    }
    NetWork ! pastryinit(numRequests:Int)
  }
}

object NetWork extends Actor {
  var b = 2 
  var ROW = 0
  var COL = 0
  var maxRequests = 0
  var nodeIds: List[String] =Nil
  var nodeList: List[PastryNode] = Nil  
  var mapIdnode = new TreeMap[String,PastryNode] 
  
  var totalhops = 0.0
  var deliveredMessages = 0.0
  var tiredNodes = 0 
  
  
  def act(){
     loop {
       react{
         case parameter(row:Int,col:Int) =>{
           ROW =row 
           COL =col
         }
         case pastryinit( numRequests:Int) => {
           initnode()
           initRoutingTable()
           startRouting(numRequests)
         }
         case MessageRecieved(hops: Int) => {
           totalhops += hops
           deliveredMessages += 1
         }
    
         case ThresholdReached => {
           tiredNodes +=1
           if(tiredNodes == nodeIds.length){
             println("Average hop per Message  ="+ (totalhops/deliveredMessages))
             System.exit(0)
           }
         } 
       }
     }
  }
  
  def startRouting(numRequests: Int){
    maxRequests = numRequests
    for (node <- nodeList){
      node ! MakeRequests(numRequests)
    }
  }
  def join(pastrynode: PastryNode){
    var id = generateId()
    while(id == ""){
      id = generateId()
    }
    pastrynode.nodeId = id
    nodeIds ::= id
    nodeList ::= pastrynode
    mapIdnode += (id -> pastrynode)
    pastrynode.start()
  }

  
  def generateId() : String = {
    var idRange = pow(2,b).toInt
    var r = new scala.util.Random
    var id = ""
    do{
      id = ""
      for(i <- 0 to ROW-1)
        id = id + r.nextInt(idRange.toInt).toString
    }while(nodeIds.contains(id))
    id
  }
  
  def initnode(){
    for(node <- nodeList){
      node ! Nodeinit(mapIdnode)
    }
  }
  
  def initRoutingTable(){
    var curid:String = ""
    var col:Int = 0
    var bestMatchString:String = ""
    for(node <- nodeList){
      curid = node.nodeId
      for(i <- 0 until ROW )
        for(j <- 0 until COL){
          col = j 
          bestMatchString = bestMatch(i,col,curid)
          node.setRoutingTableEntry( i, j, bestMatchString)
         // println("nodeiD:"+curid+"\troutingtable(i)(j):\t"+"i:"+i+"\tj:"+j+"\t"+node.routingTable(i)(j))
        }
 //     println("ID:"+node.nodeId)

    }
    
  }
  
  def bestMatch(RowNo:Int, col:Int, curid:String):String={
    var allmask:String = "ffffffff"
    NetWork.nodeIds.foreach(
      e => {
        //println("nodeId:\t"+e)
        //println("RowNo:\t"+RowNo+"\tcol:\t"+col+"\tcurid:\t"+curid)
        //println(e.regionMatches(0, curid, 0, RowNo))
        if(e.regionMatches(0, curid, 0, RowNo) && e.charAt(RowNo) == col + 48  && e!=curid)
          return e
      }
    )
    return allmask  
  }
  
 
}

class PastryNode(numNodes: Int) extends Actor{
  var b = 2
  var L =16
  var ROW = ceil(log(numNodes) / log(pow(2,b))).toInt
  var COL = pow(2,b).toInt
  var routingTable = Array.ofDim[String](ROW, COL)
  var leafset:List[String] = Nil
  var nodeId:String = "x"
  var maxRequests = 0 
 
  for(i <- 0 until ROW)
        for(j <- 0 until COL)
          routingTable(i)(j) = "ffffffff"
    
  
  def act(){
    loop {
      react{
        case Nodeinit(mapIdnode:TreeMap[String, PastryNode]) => initLeafSet(mapIdnode)
        case MakeRequests(numRequests: Int) => makeRequests(numRequests)
        case RemindMe(requestsMade: Int) => remindMe(requestsMade)
        case DoRouting(message:Message, destNode: String) => doRouting(message, destNode)
        case Deliver(message: Message) => {NetWork ! MessageRecieved(message.hop)}
      }
    }
  }
 
  def selectNode(): String = {
    var list = NetWork.nodeIds
    var destNode = ""
    do{
      list = Random.shuffle(list)
      destNode = list.head
    }while(nodeId == destNode)
    destNode
  }
  
  def makeRequests(numRequests:Int){
    maxRequests = numRequests
    NetWork.mapIdnode(nodeId) ! RemindMe(0)
  }
  def remindMe(requestsMade: Int){
    var destNode = selectNode()
    NetWork.mapIdnode(nodeId) ! DoRouting(new Message(nodeId), destNode)
    if (requestsMade == maxRequests){
      NetWork ! ThresholdReached
    }
    else{
      Thread.sleep(1000)
      NetWork.mapIdnode(nodeId) ! RemindMe(requestsMade + 1)
    }
  }
  def doRouting(message: Message,destNodeId: String){
    var KeyPrev:String = ""
    var KeyNext:String = nodeId
    do{
      /*
      if(message.hopNodes.contains(destNodeId)){
        println("Race Detected"+message.hopNodes)
      }*/
      message.hopNodes ::= KeyNext
      KeyPrev = KeyNext
      KeyNext = NetWork.mapIdnode(KeyPrev).forward(destNodeId, message)
    }while(KeyPrev != KeyNext)
    if(KeyPrev == KeyNext){ 
     // println("Race Detected:\t"+message.hopNodes)
      NetWork.mapIdnode(destNodeId) ! Deliver(message)
    }
  }
  
  def initLeafSet(mapIdnode0:TreeMap[String, PastryNode]){
    var L:Int = 16
    var tempList:List[String] = Nil
    mapIdnode0.foreach(e =>{
      tempList ::= e._1
    })
    var nodeIds =tempList.reverse.toArray
    var startIndex, endIndex = -1
    for (n <- 0 to nodeIds.size - 1){
      if (nodeIds(n) == nodeId){
        if (n > 0){
          endIndex = n - 1
          if (n - L/2 < 0)
            startIndex = 0
          else
            startIndex = n - L/2
          for (i <- startIndex to endIndex)
            leafset ::= nodeIds(i)
        }
        if (n < nodeIds.size - 1){
          startIndex = n + 1
          if (n + L/2 > nodeIds.size - 1)
            endIndex = nodeIds.size - 1
          else
            endIndex = n + L/2
          for (i <- startIndex to endIndex)
            leafset ::= nodeIds(i)
        }
      }
    }
  //  println("nodeId:  "+nodeId+"leafset:  "+leafset)
  }
  
  def forward(key:String, message:Message):String = {
    var nextNodeId:String = nodeId
    var leafarray = leafset.toArray
    scala.util.Sorting.quickSort(leafarray)
    var min_diff = strdiff(key,nextNodeId)
    if(nodeId == key){
      return key
    }
    var min_leaf = leafarray.head
    var max_leaf = leafarray.last
   // println("min_leaf:"+min_leaf+"\tmax_leaf:"+max_leaf)
    if(key.compareTo(min_leaf) >= 0 && key.compareTo(max_leaf) <= 0){
       for(c <- leafset){
         if(strdiff(key,c) < min_diff ){
           nextNodeId = c
           min_diff = strdiff(key,c);
         }
       }
       message.hop = message.hop + 1
       return nextNodeId
    }
    var row = findcommonprefix(key,nodeId)
    var col = key(row) - 48
    
    if(routingTable(row)(col).toString() != "ffffffff"){ 
      message.hop = message.hop + 1
      return routingTable(row)(col).toString()
    }
    
    
     for(i <- 0 until leafarray.length){
       if(strdiff(key,leafarray(i))  < min_diff && findcommonprefix(key,leafarray(i))>=row ){  
         nextNodeId = leafarray(i)
         min_diff = strdiff(key,leafarray(i))
       }
     }
     for(i <-0 until ROW){  
       for(j <- 0 until COL){
         if(routingTable(i)(j)!= "ffffffff"){
           if(strdiff(key,routingTable(i)(j)) < min_diff && findcommonprefix(key,routingTable(i)(j))>=row){
             min_diff = strdiff(key,routingTable(i)(j))
             nextNodeId = routingTable(i)(j)
           }
         }
       }
     }
     message.hop = message.hop + 1
     return nextNodeId
  }
  def strdiff(key:String,nextNodeId:String):Int = {
    return abs(key.toInt - nextNodeId.toInt)
  }
  
  def setRoutingTableEntry( i:Int, j:Int, bestMatchString:String){
    routingTable(i)(j) = bestMatchString
  }
  
  def findcommonprefix(id1: String, id2: String): Int = {
    var i:Int = 0
    var length:Int = id1.length()
    while(i < length){
      if(id1(i) == id2(i)){
        i +=1
      }else{
         return i
      }
    }
    return i
  }
}
