package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

public class ResultSet {
    //    以一个列表来存储所有的中间结果集，支持度不同的项集用不同的跳表结构来存储
    public SkipList[] ClosedTable;

    /*
     * constructor
     * */
    public ResultSet(int size) {
//        ClosedTable = new HashSet[size];
        ClosedTable = new SkipList[size];
    }

    /**
     * Add an itemset.
     *
     * @param citemset the itemset to be added to the hashtable
     * @param support  the hashcode of the itemset (need to be calculated before by using the
     *                 provided hashcode() method.
     */
    public void put(CItemset citemset, int support) {
        // if the position in the array is empty create a new array list
        // for that position
        if (ClosedTable[support] == null) {
            ClosedTable[support] = new SkipList();
        }
        // store the itemset in the arraylist of that position
        ClosedTable[support].insert(citemset);
    }

    /**
     * 寻找一个项集
     */
    public CItemset retrieveItemset(int[] items, int support) {
        CItemset citemset = new CItemset(items);
        if (ClosedTable[support] == null || ClosedTable[support].getNodecount() == 0) {
            return null;
        }
        return ClosedTable[support].findCItemset(citemset);
//        if (ClosedTable[support].find(citemset) != null)
//            return ClosedTable[support].find(citemset).getcItemset();
//        else
//            return null;
    }

    /*
     * 从Closed table中删除一个项集
     * @param itemset the itemset to be added to the hashtable
     * */
    public void remove(CItemset citemset, int support) {
        ClosedTable[support].delete(citemset);
    }
}
