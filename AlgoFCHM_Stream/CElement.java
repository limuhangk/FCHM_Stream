package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

/**
 * 效用列表中的元组
 * 存储 tid,eu,ru,ext
 * ext结构 存储 在当前事务中，当前项之后能够用来扩展的项的个数，方便识别压缩效用
 * 当满足C(X-xk) = S(Tj/X-xk)时，项集的效用为 complete utility
 */
public class CElement {
    // The three variables as described in the paper:
    /** transaction id */
//    public final int tid ;
    public final int tid ;
    /** itemset utility */
    public final int iutils;
    /** prefix utility */
//    public int putility;
    /** remaining utility */
    public int rutils;
    /** 事务中该项集下一项的位置*/
//    public int ext;
    /**  下一项在其效用列表中的偏移量     */
//    public int offset;
    /**
     * Constructor.
     * @param tid  the transaction id
     * @param iutils  the itemset utility
     */
    public CElement(int tid, int iutils, int rutils){
        this.tid = tid;
        this.iutils = iutils;
        this.rutils = rutils;
    }

    /**
     *
     * @param tid
     * @param iutils
     * @param rutils
     * @param ext
     */
//    public CElement(int tid, int iutils, int rutils, int ext){
//        this.tid = tid;
//        this.iutils = iutils;
//        this.rutils = rutils;
//        this.ext = ext;
//    }

    /**
     *
     * @param tid
     * @param iutils
     * @param rutils
     * @param ext
     * @param offset
     */
//    public CElement(int tid, int iutils, int rutils, int ext, int offset){
//        this.tid = tid;
//        this.iutils = iutils;
//        this.rutils = rutils;
//        this.ext = ext;
//        this.offset = offset;
//    }
//    public CElement(int tid, int iutils, int rutils, int putility,int ext, int offset){
//        this.tid = tid;
//        this.iutils = iutils;
//        this.rutils = rutils;
//        this.putility = putility;
//        this.ext = ext;
//        this.offset = offset;
//    }
}
