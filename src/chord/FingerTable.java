package chord;

import models.Peer;

public class FingerTable {

    private int start;
    private int begin;
    private int end;
    private Peer successor;

    
    /** 
     * @param newStart
     */
    
    /** 
     * @param newStart
     */
    // Setters
    
    /** 
     * @param begin
     * @param end
     */
    public void setStart(int newStart) {
        this.start = newStart;
    }

    
    
    /** 
     * @param newSuccessor
     */
    /** 
     * @param begin
     * @param end
     */
    
    /** 
     * @return int
     */
    public void setInterval(int begin, int end) {
        this.begin = begin;
        this.end = end;
    }

    
    /** 
     * @return int
     */
    
    /** 
     * @param newSuccessor
     */
    
    /** 
     * @return int
     */
    public void setSuccessor(Peer newSuccessor) {
        this.successor = newSuccessor;
    }

    
    /** 
     * @return Peer
     */
    
    /** 
     * @return int
     */
    // Getters
    public int getStart() {
        return this.start;
    }

    
    /** 
     * @return int
     */
    public int getIntervalBegin() {
        return this.begin;
    }

    
    /** 
     * @return int
     */
    public int getIntervalEnd() {
        return this.end;
    }

    
    /** 
     * @return Peer
     */
    public Peer getSuccessor() {
        return this.successor;
    }

    public FingerTable() {
    }

    public FingerTable(int startID, Peer succ) {
        start = startID;
        successor = succ;
    }
}
