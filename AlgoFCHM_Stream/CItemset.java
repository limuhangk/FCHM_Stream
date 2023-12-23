package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

import ca.pfv.spmf.algorithms.frequentpatterns.HMiner_CLosed.Itemset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CItemset {
    /**
     * the array of items
     **/
    public int[] citemset;
    /*    *//**
     * the oldest batch utility of this itemset
     *//*
    public double oldBatchUtility = 0;*/
    /**
     * the utility of this itemset
     */
    public double utility = 0;

    /**
     * the support of this itemset
     */
    public int support = 0;

    /**
     * the length of this itemset
     */
    public int length = 0;
    /*
     * 是否在旧批次中
     * */
    public boolean isBelongToOldBatch = false;
    // the list of elements in this utility list
    public List<CElement> elements = new ArrayList<CElement>();

    /**
     * Get the items as array
     *
     * @return the items
     */
    public int[] getItems() {
        return citemset;
    }


    public CItemset(int[] itemset, int utility, int support, CList utilityList) {
        this.citemset = itemset;
        this.utility = utility;
        this.support = support;
        for (CElement element : utilityList.elementsC)
            this.elements.add(element);
        for (CElement element : utilityList.elementsN)
            this.elements.add(element);
    }

    public CItemset() {
        citemset = new int[]{};
    }

    public CItemset(int item, int length) {
        citemset = new int[]{item};
        this.length = length;
    }

    /**
     * Constructor
     *
     * @param item an item that should be added to the new itemset
     */
    public CItemset(int item) {
        citemset = new int[]{item};
    }

    /**
     * Constructor
     *
     * @param items an array of items that should be added to the new itemset
     */
    public CItemset(int[] items) {
        this.citemset = items;
    }

    /**
     * Constructor
     *
     * @param itemset a list of Integer representing items in the itemset
     * @param utility the utility of the itemset
     */
    public CItemset(int[] itemset, double utility, int support) {
        this.citemset = itemset;
        this.utility = utility;
        this.support = support;
    }

    public CItemset(List<Integer> itemset, double utility) {
        this.citemset = new int[itemset.size()];
        int i = 0;
        for (Integer item : itemset) {
            this.citemset[i++] = item.intValue();
        }
        this.utility += utility;
    }

    /**
     * Constructor
     *
     * @param itemset a list of Integer representing items in the itemset
     * @param utility the utility of the itemset
     * @param support
     */
    public CItemset(List<Integer> itemset, double utility, int support) {
        this.citemset = new int[itemset.size()];
        int i = 0;
        for (Integer item : itemset) {
            this.citemset[i++] = item.intValue();
        }
        this.utility = utility;
        this.support = support;
    }

    /**
     * Constructor
     *
     * @param itemset
     * @param utility
     */
    public CItemset(int[] itemset, double utility) {
        this.citemset = itemset;
        this.utility = utility;
    }

    public CItemset updateCitemset(CList clist) {
        for (CElement element : clist.elementsN) {
            this.elements.add(element);
            this.utility += element.iutils;
            this.support++;
        }
        return this;
    }

    //寻找tid所对应的element
//    public boolean ifcontaintid(int tid) {
//        for (CElement celt : elements) {
//            if (celt.tid == tid)
//                return true;
//        }
//        return false;
//    }
/*    public boolean ifcontaintid(int tid) {
        if (findElementtotid(tid) != null)
            return true;
        return false;
    }*/

    /**
     * Method to remove an tid to this utility list and update the sums at the same time.
     */
/*    public void removetid(int tid) {
        Pair celement = findElementtotid(tid);
        this.utility -= celement.util;
        this.support -= 1;
        this.Pelement.remove(celement);
    }*/
    public void removetid(int position) {
        CElement element = elements.get(position);
        this.utility -= elements.get(position).iutils;
        this.support -= 1;
        this.elements.remove(element);
    }

    /*public CElement findElementtotid(int tid) {
        int first = 0;
        int last = this.elements.size() - 1;
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2
            if (elements.get(middle).tid < tid) {
                first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            } else if (elements.get(middle).tid > tid) {
                last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return elements.get(middle);
            }
        }
        return null;
    }*/

    //寻找tid所对应的element
//    public CElement findElementtotid(int tid) {
//        CElement celement = new CElement(tid, 0, 0);
//        for (CElement celt : elements) {
//            if (celt.tid == tid)
//                celement = celt;
//        }
//        return celement;
//    }

/*    //判断的是支持度相同的两个项集，所以不必判断两个项集的TidSet的大小是否相同
    public boolean ifTidSetequal(CItemset itemset2) {
        HashSet<Integer> hashset = new HashSet<>();
        for (int i = 0; i < this.elements.size(); i++) {
            if (!hashset.contains(this.elements.get(i).tid))
                hashset.add(this.elements.get(i).tid);
        }
        for (int j = 0; j < itemset2.elements.size(); j++)
            if (!hashset.contains(itemset2.elements.get(j).tid))
                return false;
        return true;
    }*/


    /*
     * get 方法
     * */
    public int[] getCItemset() {
        return citemset;
    }

    /*返回项集的长度*/
    public int getlength() {
        return this.citemset.length;
    }

    /**
     * Get the support of this itemset
     */
    public int getSupport() {
        return support;
    }

    /**
     * Get the utility of this itemset
     */
    public double getUtility() {
        return utility;
    }

    /**
     * Get the size of this itemset
     */
    public int size() {
        return citemset.length;
    }

    /**
     * Get the item at a given position in this itemset
     */
    public Integer get(int position) {
        return citemset[position];
    }
    /*
     * set 方法
     * */

    /**
     * Set the utility of this itemset
     *
     * @param utility the utility
     */
    public void setUtility(double utility) {
        this.utility = utility;
    }

    /**
     * Set the support of this itemset
     *
     * @param support the support
     */
    public void setSupport(Integer support) {
        this.support = support;
    }

    /**
     * Get a string representatino of this transaction
     *
     * @return a string
     */
    public String toString() {
        // use a string buffer for more efficiency
        StringBuffer r = new StringBuffer();
        // for each item, append it to the stringbuffer
        for (int i = 0; i < size(); i++) {
            r.append(get(i));
            r.append(' ');
        }
        r.append("#util: ");
        r.append(utility);
        r.append(" #sup: ");
        r.append(support);
        return r.toString(); // return the tring
    }

    /**
     * This method compare this itemset with another itemset to see if they are
     * equal. The method assume that the two itemsets are lexically ordered.
     *
     * @param citemset2 an itemset
     * @return true or false
     */
    public boolean isEqualTo(CItemset citemset2) {
        // If they don't contain the same number of items, we return false
        if (this.size() != citemset2.size()) {
            return false;
        }
        // We compare each item one by one from i to size - 1.
        for (int i = 0; i < citemset2.size(); i++) {
            // if different, return false
            if (!citemset2.get(i).equals(this.get(i))) {
                return false;
            }
        }
        // All the items are the same, we return true.
        return true;
    }

    //比较两个项集，判断一个项集是否是另一个项集的子集
    public boolean isSubset(CItemset itemset2) {
        HashSet<Integer> hashset = new HashSet<>();
        for (int i = 0; i < this.citemset.length; i++) {
            if (!hashset.contains(this.citemset[i]))
                hashset.add(this.citemset[i]);
        }
        for (int j = 0; j < itemset2.citemset.length; j++)
            if (!hashset.contains(itemset2.citemset[j]))
                return false;
        return true;
    }

    public boolean contains(CItemset itemset2) {
        int length1 = this.getlength();
        int length2 = itemset2.getlength();
        itemset2.quickSort(itemset2.getItems(), 0, length2 - 1);
        if (this.citemset[length1 - 1] < itemset2.getItems()[length2 - 1])
            return false;
        int j = 0;
        for (int i = 0; j < length2 && i < length1; i++) {
            if (this.citemset[i] > itemset2.getItems()[j])
                return false;
            else if (this.citemset[i] == itemset2.getItems()[j])
                j++;
            else
                continue;
        }
        if (j == length2)
            return true;
        else
            return false;
    }

    /**
     * quickSort
     *
     * @param arr
     * @param low
     * @param high
     */
    public void quickSort(int[] arr, int low, int high) {
        int i, j, temp, t;
        if (low > high) {
            return;
        }
        i = low;
        j = high;
        //temp就是基准位
        temp = arr[low];

        while (i < j) {
            //先看右边，依次往左递减
            while (temp <= arr[j] && i < j) {
                j--;
            }
            //再看左边，依次往右递增
            while (temp >= arr[i] && i < j) {
                i++;
            }
            //如果满足条件则交换
            if (i < j) {
                t = arr[j];
                arr[j] = arr[i];
                arr[i] = t;
            }

        }
        //最后将基准为与i和j相等位置的数字交换
        arr[low] = arr[i];
        arr[i] = temp;
        //递归调用左半数组
        quickSort(arr, low, j - 1);
        //递归调用右半数组
        quickSort(arr, j + 1, high);
    }
}
