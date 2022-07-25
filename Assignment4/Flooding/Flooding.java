import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class Flooding {
    public static void main(String[] args) {
        
        Node nodeS = new Node("S");
        Node nodeA = new Node("A");
        Node nodeB = new Node("B");
        Node nodeC = new Node("C");
        Node nodeE = new Node("E");
        Node nodeF = new Node("F");
        Node nodeG = new Node("G");
        Node nodeH = new Node("H");
        Node nodeI = new Node("I");
        Node nodeJ = new Node("J");
        Node nodeK = new Node("K");
        Node nodeL = new Node("L");
        Node nodeM = new Node("M");
        Node nodeN = new Node("N");
        Node nodeD = new Node("D");
        
        nodeS.connect(nodeB, nodeC, nodeE);
        nodeA.connect(nodeB, nodeH);
        nodeB.connect(nodeH);
        nodeC.connect(nodeE, nodeG, nodeH);
        nodeE.connect(nodeF);
        nodeF.connect(nodeG, nodeJ);
        nodeG.connect(nodeK);
        nodeH.connect(nodeI);
        nodeJ.connect(nodeM, nodeD);
        nodeK.connect(nodeD);
        nodeL.connect(nodeM);
        nodeN.connect(nodeD);
        
        nodeD.setDestinatinNode(true);
        nodeS.receiveAndTransmit(new Packet());
    }
}

class Node {
    private String name;
    private boolean isDestNode = false;
    private Set<Node> connectedNodes = new HashSet<Node>();
    
    public Node(String name) {
        this.name = name;
    }
     
    public void connect(Node... nodes) {
        for (Node node: nodes) {
            this.connectedNodes.add(node);
            node.connectedNodes.add(this);
        }
    }

     public void setDestinatinNode(boolean isDestNode) {
        this.isDestNode = isDestNode;
    }
 
    public void receiveAndTransmit(Packet packet) {
        // Append current node to route array
        packet.getRoute().add(this.name);
        
        // Checks if destination node was reached
        if (this.isDestNode) {
            System.out.print("Destination node reached. Transmission route: " + packet.printRoute() + "\n");
        }
        else {
            boolean areThereNodesToTransmit = false;
            // Iterate connected nodes and transmits packet to nodes that haven't received it yet
            for (Node nextNode: connectedNodes) {
                if (!packet.getRoute().contains(nextNode.name)) {
                    areThereNodesToTransmit = true;
                    nextNode.receiveAndTransmit(packet.clone());
                }
            }
            // If node has no nodes to transmit, transmission is over
            if (!areThereNodesToTransmit) {
                //System.out.print("Transmission stopped, destination node not reached. " + packet.printRoute() + "\n");
            }
        }
    }
 }

 class Packet implements Cloneable {
    // Transmission route (array of nodes that were traversed)
    private ArrayList<String> route = new ArrayList<String>();

    public List<String> getRoute() {
        return route;
    }
    
    // Creates a clone of previous packet to transmit it
    @SuppressWarnings("unchecked")
    public Packet clone() {
        Packet clonePacket = null;
        try {
            clonePacket = (Packet) super.clone();
            clonePacket.route = (ArrayList<String>) this.route.clone();
        } catch (CloneNotSupportedException exc) {
            exc.printStackTrace();
        }
        return clonePacket;
    }
    
    public String printRoute() {
        return String.format("%s", route);
    }
     
 }