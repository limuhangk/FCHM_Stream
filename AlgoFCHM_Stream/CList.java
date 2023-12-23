package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 压缩列表  Compressed UtilityList
 * 列表中包含两个TidSet 分别是 elementsC 以及 elementsN ，代表旧批次（包含最旧批次与公共批次） 以及最新批次
 * 具有 CU/CRU/CPU三个 complete utility 来压缩存储<tid,eu,ru>
 * 一项集压缩列表构建过程中，仅压缩同一批次当中的事务，避免窗口滑动导致删除批次外的事务
 */
public class CList {
    /**
     * this variable stores the sum of SU+RU for each batch
     */
//    int[] batchUAR;
//    ArrayList<Integer> batchUAR;
//    HashMap<Integer,Integer> batchUAR = new HashMap<>();
    // the last item of the itemset represented by this utility list
    public Integer item;
    // the sum of iutil values of common batchs
    public int sumIutilsC = 0;
    // the sum of rutil values of common batchs
    public int sumRutilsC = 0;

    // the sum of iutil values of new batch
    public int sumIutilsN = 0;
    // the sum of rutil values of new batch
    public int sumRutilsN = 0;
    /**
     * the sum of prefix utilities
     */
//    public int sumPutitliy = 0;
    /**
     * the sum of Closed utilities
     */
    // the list of elements in this utility list
    public List<CElement> elementsC = new ArrayList<CElement>();
    // the list of elements in this utility list
    public List<CElement> elementsN = new ArrayList<CElement>();

//	public List<Itemset> closedItemset = new ArrayList<Itemset>();

    /**
     * Constructor.
     *
     * @param item the item that is used for this utility list
     */
    public CList(Integer item) {
        this.item = item;
    }

//    /**
//     * 初始化列表中的batchUAR，将每个列表划分为win_size大小
//     *
//     * @param item
//     * @param win_size
//     */
//    public CList(Integer item, int win_size) {
//        this.item = item;
////        this.batchUAR = new int[win_size];
//        this.batchUAR = new ArrayList<>(win_size);
//        for(int i = 0;i<win_size;i++)
//            batchUAR.add(0);
//    }

    public CList(CList utilityList) {
        this.item = item;
        this.elementsN = utilityList.elementsN;
        this.elementsC = utilityList.elementsC;
    }

    /**
     * Method to add an element to this utility list and update the sums at the same time.
     */
    public void addElementC(CElement element) {
        sumIutilsC += element.iutils;
        sumRutilsC += element.rutils;
//        sumPutitliy += element.putility;
        elementsC.add(element);
    }

//    public void addElementC(CElement element, int batch_size) {
//        sumIutilsC += element.iutils;
//        sumRutilsC += element.rutils;
////        sumPutitliy += element.putility;
//        elementsC.add(element);
//        // update the sum of utility and remaining utility for the batch
//        // corresponding to this element
//        int partition = (element.tid - 1) / batch_size;
//        batchUAR.add(partition, element.iutils + element.rutils);
////        batchUAR[partition] += element.iutils + element.rutils;
//    }

    /**
     * Method to add an element to this utility list and update the sums at the same time.
     */
    public void addElementN(CElement element) {
        sumIutilsN += element.iutils;
        sumRutilsN += element.rutils;
//        sumPutitliy += element.putility;
        elementsN.add(element);
    }

//    public void addElementN(CElement element, int batch_size, int win_size) {
//        sumIutilsN += element.iutils;
//        sumRutilsN += element.rutils;
////        sumPutitliy += element.putility;
//        elementsN.add(element);
//        // update the sum of utility and remaining utility for the batch
//        // corresponding to this element
//        int partition = (element.tid - 1) / batch_size;
//        if (partition < win_size)
//            batchUAR.add(0);
//    }

    /**
     * Method to remove an element to this utility list and update the sums at the same time.
     */
    public void removeElement(CElement element) {
        sumRutilsC -= element.iutils;
        elementsC.remove(element);
    }

    /**
     * Method to remove an tid to this utility list and update the sums at the same time.
     */
    public void removetid(int tid) {
//        CElement element = findElementtotid(tid);
        CElement element = findElementtotidC(tid);
        sumIutilsC -= element.iutils;
        elementsC.remove(element);
    }

    /**
     * Method to remove an element to this utility list and update the sums at the same time.
     */
    public void removeElementN(CElement element) {
        sumRutilsN -= element.iutils;
        elementsN.remove(element);
    }

    //寻找tid所对应的element
//    public CElement findElementtotid(int tid) {
////        CElement element = new CElement(tid, 0, 0, 0);
//        CElement element = new CElement(tid, 0, 0);
//        for (CElement elt : elementsC) {
//            if (elt.tid == tid)
//                element = elt;
//        }
//        return element;
//    }

    //寻找tid所对应的element
    public boolean ifcontaintid(int tid) {
        if(findElementtotidC(tid)!=null)
            return true;
        if(findElementtotidN(tid)!=null)
            return true;
        return false;
    }
//    public boolean ifcontaintid(int tid) {
//        for (CElement elt : elementsC) {
//            if (elt.tid == tid)
//                return true;
//        }
//        for (CElement elt : elementsN) {
//            if (elt.tid == tid)
//                return true;
//        }
//        return false;
//    }

    public CElement findElementtotidC(int tid) {
//        CElement element = new CElement(tid, 0, 0, 0);
        int first = 0;
        int last = elementsC.size() - 1;
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2

            if (elementsC.get(middle).tid < tid) {
                first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            } else if (elementsC.get(middle).tid > tid) {
                last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return elementsC.get(middle);
            }
        }
        return null;
    }
    public CElement findElementtotidN(int tid) {
//        CElement element = new CElement(tid, 0, 0, 0);
        int first = 0;
        int last = elementsN.size() - 1;
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2

            if (elementsN.get(middle).tid < tid) {
                first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            } else if (elementsN.get(middle).tid > tid) {
                last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return elementsN.get(middle);
            }
        }
        return null;
    }

    public void switchNtoC() {
        sumIutilsC += sumIutilsN;
        sumIutilsN = 0;
        elementsC.addAll(elementsN);
        elementsN.clear();
    }

    /**
     * Get the support of the itemset represented by this utility-list
     *
     * @return the support as a number of trnsactions
     */
    public int getSupport() {
        return elementsC.size() + elementsN.size();
    }

    public String toString() {
        StringBuffer r = new StringBuffer();
        r.append(this.item);
        r.append("\n");
        for (CElement ele : this.elementsC) {
            r.append(ele.tid + " " + ele.iutils + " " + ele.rutils + "\n");
        }
        r.append("\n");
        for (CElement ele : this.elementsN) {
            r.append(ele.tid + " " + ele.iutils + " " + ele.rutils + "\n");
        }
        return r.toString();
    }
}
