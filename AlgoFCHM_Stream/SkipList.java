package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * 1，跳表的一种实现方法，用于练习。跳表中存储的是正整数，并且存储的是不重复的。
 * 2，本类是参考作者zheng ，自己学习，优化了添加方法
 * 3，看完这个，我觉得再看ConcurrentSkipListMap 源码，会有很大收获
 * Author：ldb
 * <p>
 * <p>
 * https://juejin.cn/post/6844903446475177998
 * https://www.jianshu.com/p/9d8296562806
 * https://juejin.cn/post/7009520476447997966
 * <p>
 * https://juejin.cn/post/6844903446475177998
 * https://www.jianshu.com/p/9d8296562806
 * https://juejin.cn/post/7009520476447997966
 * <p>
 * https://juejin.cn/post/6844903446475177998
 * https://www.jianshu.com/p/9d8296562806
 * https://juejin.cn/post/7009520476447997966
 */

/**
 * https://juejin.cn/post/6844903446475177998
 * https://www.jianshu.com/p/9d8296562806
 * https://juejin.cn/post/7009520476447997966
 */

/**
 * 跳表中存储支持度相同的项集，存储的时候实际上就按照项集本身来存储就行，
 * 不用按照长度来区分，结果集中不存在相同的项集
 * 所以也就没有必要区分是否有相同长度的项集。
 */
public class SkipList {

    private static final int MAX_LEVEL = 16;
    private int levelCount = 1;//跳表的层数
    private int Nodecount = 0;
    /**
     * 带头链表
     */
    private Node head = new Node(MAX_LEVEL);//头节点
    private Node tail;
    private Random r = new Random();

    /**
     * 由于TWU顺序随着批次的到来不断变化，按照顺序寻找的项集有些会找不到，这是由于 islessthan函数无法判断顺序变化的项集所致，
     * 下一步准备将列表与项集按照字典顺序实现。字典顺序整个算法直接被拖慢了
     * 使用两个快速排序解决旧项集无法被找到导致没有删除的问题
     * @param cItemset1
     * @return
     */
    public Node find(CItemset cItemset1) {
        cItemset1.quickSort(cItemset1.citemset, 0, cItemset1.getlength() - 1);
        Node p = head;
        // 从最大层开始查找，找到前一节点，通过--i，移动到下层再开始查找
        for (int i = levelCount - 1; i >= 0; --i) {
            while (p.forwards[i] != null && islessthan(p.forwards[i].cItemset.citemset, cItemset1.citemset)) {
                // 找到前一节点
                p = p.forwards[i];
            }
        }
        if (p.forwards[0] != null && Arrays.equals(p.forwards[0].cItemset.citemset, cItemset1.citemset)) {
//        if (p.forwards[0] != null && isSame(p.forwards[0].cItemset.citemset, cItemset1.citemset)) {
            return p.forwards[0];
        } else {
            return null;
        }
    }
    public CItemset findCItemset(CItemset cItemset1) {
        cItemset1.quickSort(cItemset1.citemset, 0, cItemset1.getlength() - 1);
        Node p = head;
        // 从最大层开始查找，找到前一节点，通过--i，移动到下层再开始查找
        for (int i = levelCount - 1; i >= 0; --i) {
            while (p.forwards[i] != null && islessthan(p.forwards[i].cItemset.citemset, cItemset1.citemset)) {
                // 找到前一节点
                p = p.forwards[i];
            }
        }
        if (p.forwards[0] != null && Arrays.equals(p.forwards[0].cItemset.citemset, cItemset1.citemset)) {
//        if (p.forwards[0] != null && isSame(p.forwards[0].cItemset.citemset, cItemset1.citemset)) {
            return p.forwards[0].cItemset;
        } else {
            return null;
        }
    }

    /**
     * 返回指定长度的项集的集合
     * @param length
     * @return
     */
    public List<CItemset> findByLength(int length) {
        List<CItemset> Set_Length = new ArrayList<>();
        Node p = head;
        // 从最大层开始查找，找到前一节点，通过--i，移动到下层再开始查找
        //找出指定长度-1的项集的位置
        for (int i = levelCount - 1; i >= 0; --i) {
            while (p.forwards[i] != null && p.forwards[i].cItemset.getlength() < length) {
                // 找到前一节点
                p = p.forwards[i];
            }
        }
//        Node fv = p.forwards[0];
        while (p.forwards[0] != null && p.forwards[0].cItemset.getlength() == length) {
            Set_Length.add(p.forwards[0].cItemset);
            p = p.forwards[0];
        }
//        Node bv = p;
//        while (bv != null && bv.cItemset.getlength() == length){
//            Set_Length.add(bv.cItemset);
//            bv = bv.backward;
//        }
        return Set_Length;
    }

    public void insert(CItemset cItemset1) {
        cItemset1.quickSort(cItemset1.citemset, 0, cItemset1.getlength() - 1);
        int level = head.forwards[0] == null ? 1 : randomLevel();
        // 每次只增加一层，如果条件满足
        if (level > levelCount) {
            level = ++levelCount;
        }
        Node newNode = new Node(level);
        newNode.cItemset = cItemset1;
        Node p = head;
        // 从最大层开始查找，找到前一节点，通过--i，移动到下层再开始查找
        for (int i = levelCount - 1; i >= 0; --i) {
//            while (p.forwards[i] != null && p.forwards[i].citemset.length < cItemset.length) {
            while (p.forwards[i] != null && islessthan(p.forwards[i].cItemset.citemset, cItemset1.citemset)) {
                // 找到前一节点
                p = p.forwards[i];
            }
            // levelCount 会 > level，所以加上判断
            if (level > i) {
                if (p.forwards[i] == null) {
                    p.forwards[i] = newNode;
                    if (i == 0)
                        tail = newNode;//尾结点
                } else {
                    Node next = p.forwards[i];
                    p.forwards[i] = newNode;
                    newNode.forwards[i] = next;
                }
            }
        }
        //构建新节点以及其后节点在最底层的向前的指针
/*        newNode.backward = (p == head) ? null : p;
        if (newNode.forwards[0] != null) {
            newNode.forwards[0].backward = newNode;
        }*/
        Nodecount++;
    }

    public void delete(CItemset cItemset1) {
        Node[] update = new Node[levelCount];
        Node p = head;
        for (int i = levelCount - 1; i >= 0; --i) {
            while (p.forwards[i] != null && islessthan(p.forwards[i].cItemset.citemset, cItemset1.citemset)) {
                p = p.forwards[i];
            }
            update[i] = p;
        }

        if (p.forwards[0] != null && Arrays.equals(p.forwards[0].cItemset.citemset, cItemset1.citemset)) {
            if (tail == p.forwards[0])//删除更新尾节点
                tail = p;
            for (int i = levelCount - 1; i >= 0; --i) {
                if (update[i].forwards[i] != null && Arrays.equals(update[i].forwards[i].cItemset.citemset, cItemset1.citemset)) {
                    update[i].forwards[i] = update[i].forwards[i].forwards[i];
                }
            }
        }
/*        if (p.forwards[0] != null)
            p.forwards[0].forwards[0].backward = p.forwards[0].backward;*/
        Nodecount--;
    }

    /**
     * 随机 level 次，如果是奇数层数 +1，防止伪随机
     *
     * @return
     */
    private int randomLevel() {
        int level = 1;
        for (int i = 1; i < MAX_LEVEL; ++i) {
            if (r.nextInt() % 2 == 1) {
                level++;
            }
        }
        return level;
    }

    /**
     * 打印每个节点数据和最大层数
     */
    public void printAll() {
        Node p = head;
        while (p.forwards[0] != null) {
            System.out.print(p.forwards[0] + " ");
            p = p.forwards[0];
        }
        System.out.println();
    }

    /**
     * 将跳表中项集写入文件
     * @param writer
     */
    public void writeToFile(Writer writer) throws IOException {
        Node p = head;
        while (p.forwards[0] != null) {
            writer.write(p.forwards[0] + "\n");
            p = p.forwards[0];
        }
    }

    /**
     * 返回跳表结构的尾结点的项集
     * @return
     */
    public CItemset getTailCItemset() {
        return this.tail.cItemset;
    }

    /**
     * 获取跳表中项集的个数
     * @return
     */
    public int getCount() {
        int count = 0;
        Node p = head;
        while (p.forwards[0] != null) {
            count++;
            p = p.forwards[0];
        }
        return count;
    }

    /**
     * 返回跳表的结点数
     * @return
     */
    public int getNodecount() {
        return this.Nodecount;
    }

    /**
     * 删除跳表中项集在旧批次中的效用
     * @param OldBatchTid
     * @param batch_size
     * @param minutil
     */
    public void removeOldBatchUtility(int OldBatchTid, int batch_size, int minutil) {
        Node p = head;
        while (p.forwards[0] != null) {
            CItemset citemset = p.forwards[0].cItemset;
            while (citemset.elements.size() != 0) {
                int tidTemp = citemset.elements.get(0).tid;
                if ((tidTemp >= OldBatchTid) && (tidTemp < (OldBatchTid + batch_size))) {
                    citemset.removetid(0);//删除该项集在该Tid的元组
                    citemset.isBelongToOldBatch = true;
                } else
                    break;
            }
            if (citemset.isBelongToOldBatch == true && (citemset.getSupport() == 0 || citemset.getUtility() < minutil))//保存中间结果中所有支持度不为0以及效用大于minutil的节点
                delete(citemset);
            else
                p = p.forwards[0];
        }
    }

    /**
     * 删除结果集中不再闭合的项集，将支持度变化的项集更新到对应的跳表中
     * @param ClosedTable
     */
    public void updateSkipList(SkipList[] ClosedTable) {
        Node p = head;
        while (p.forwards[0] != null) {
            CItemset citemset1 = p.forwards[0].cItemset;
            if (citemset1.isBelongToOldBatch == true) {//仅需检查那些标志为true的项集，他们同时出现在旧批次与公共批次当中
                boolean isclosed = true;
                if (ClosedTable[citemset1.support] != null) {
                    SkipList skipList2 = ClosedTable[citemset1.support];//找到该项集的支持度所在的跳表
                    for (int len = citemset1.getlength() + 1; isclosed && len <= skipList2.tail.cItemset.getlength(); len++) {
                        for (CItemset citemset2 : skipList2.findByLength(len))
                            if (citemset2.isBelongToOldBatch == false) {//仅需检查那些标志为flase的项集，这些项集仅出现在公共批次中
                                //若两个项集的tidset相同 且 一个项集为另一个项集的支持度相同的超集，则删除该项集,
                                if (citemset2.contains(citemset1) == true) {
                                    isclosed = false;
                                    delete(citemset1);
                                    break;
                                }
                            }
                    }
                }
                //否则将该项集加入到与其支持度相同的结果集的列表当中
                if (isclosed == true) {
                    if (ClosedTable[citemset1.support] == null)
                        ClosedTable[citemset1.support] = new SkipList();
                    ClosedTable[citemset1.support].insert(citemset1);
                    delete(citemset1);//从原列表当中删除
                    citemset1.isBelongToOldBatch = false;
                }
            } else
                p = p.forwards[0];
        }
    }

    /**
     * 将结果集中因新批次加入导致支持度以及效用变化的项集更新
     * 删除结果集中不再闭合的项集，将支持度变化的项集更新到对应的跳表中
     * @param ClosedTable
     */
    public void updateNewBatchSkipList(SkipList[] ClosedTable) {
        Node p = head;
        while (p.forwards[0] != null) {
            CItemset citemset1 = p.forwards[0].cItemset;
            if (citemset1.isBelongToOldBatch == true) {//仅需检查那些标志为true的项集，他们同时出现在旧批次与公共批次当中
                boolean isclosed = true;
                if (ClosedTable[citemset1.support] != null) {
                    SkipList skipList2 = ClosedTable[citemset1.support];//找到该项集的支持度所在的跳表
                    for (int len = citemset1.getlength() + 1; isclosed && len <= skipList2.tail.cItemset.getlength(); len++) {
                        for (CItemset citemset2 : skipList2.findByLength(len))
                            if (citemset2.isBelongToOldBatch == false) {//仅需检查那些标志为flase的项集，这些项集仅出现在公共批次中
                                //若两个项集的tidset相同 且 一个项集为另一个项集的支持度相同的超集，则删除该项集,
                                if (citemset2.contains(citemset1) == true) {
                                    isclosed = false;
                                    delete(citemset1);
                                    break;
                                }
                            }
                    }
                }
                //否则将该项集加入到与其支持度相同的结果集的列表当中
                if (isclosed == true) {
                    if (ClosedTable[citemset1.support] == null)
                        ClosedTable[citemset1.support] = new SkipList();
                    ClosedTable[citemset1.support].insert(citemset1);
                    delete(citemset1);//从原列表当中删除
                    citemset1.isBelongToOldBatch = false;
                }
            } else
                p = p.forwards[0];
        }
    }

    /**
     * 判断两个数组是否相同，无序
     * @param itemset1
     * @param itemset2
     * @return
     */
    public static boolean isSame(int[] itemset1, int[] itemset2) {
        if (itemset1.length != itemset2.length) {
            return false;
        }
        // Otherwise, we have to compare item by item
        int i = 0;
        // for each item in itemset2, we will try to find it in itemset 1
        for (int j = 0; j < itemset2.length; j++) {
            boolean found = false; // flag to remember if we have find the item at position j

            // we search in this itemset starting from the current position i
            while (found == false && i < itemset1.length) {
                // if we found the current item from itemset2, we stop searching
                if (itemset1[i] == itemset2[j]) {
                    found = true;
                }

                i++; // continue searching from position  i++
            }
            // if the item was not found in the previous loop, return false
            if (!found) {
                return false;
            }
            i = 0;
        }
//		// All items are the same. We return true.
        return true;
    }

    /**
     *
     * @param itemset1
     * @param itemset2
     * @return true itemset1 < itemset2
     *         false itemset > itemset2
     */
    public static boolean islessthan(int[] itemset1, int[] itemset2) {
        if (itemset1.length > itemset2.length) {
            return false;
        } else if (itemset1.length < itemset2.length) {
            return true;
        } else {
            // Otherwise, we have to compare item by item
            // for each item in itemset2, we will try to find it in itemset 1
            if (Arrays.equals(itemset1, itemset2))
//            if (isSame(itemset1, itemset2))
                return false;
            for (int j = 0; j < itemset2.length; j++) {
                // we search in this itemset starting from the current position i
                if (itemset1[j] == itemset2[j])
                    continue;
                else if (itemset1[j] < itemset2[j])
                    return true;
                else if (itemset1[j] > itemset2[j])
                    return false;
            }
            return false;
        }
    }


    /**
     * 跳表的节点，每个节点记录了当前节点数据和所在层数数据
     */
    public class Node {
        private CItemset cItemset = new CItemset(0, -1);
        /**
         * 表示当前节点位置的下一个节点所有层的数据，从上层切换到下层，就是数组下标-1，
         * forwards[3]表示当前节点在第三层的下一个节点。
         */
        private Node forwards[];//所有前向指针
//        private Node backward;//最底层的后向指针，相当于在最底层的跳表是一个双向的列表
        /**
         * 这个值其实可以不用，看优化insert()
         */
        private int maxLevel = 0;

        public Node(int level) {
            forwards = new Node[level];
//            backward = null;
        }

        @Override
        public String toString() {
            // use a string buffer for more efficiency
            StringBuffer r = new StringBuffer();
            // for each item, append it to the stringbuffer
            for (int i = 0; i < cItemset.size(); i++) {
                r.append(cItemset.get(i));
                r.append(' ');
            }
            r.append("#util: ");
            r.append(cItemset.utility);
            r.append(" #sup: ");
            r.append(cItemset.support);
            return r.toString(); // return the tring
        }

        public CItemset getcItemset() {
            return cItemset;
        }
    }

    /**
     * test main
     */
/*    public static void main(String[] args) {
        SkipList skipList = new SkipList();
//        List<CItemset> Set_Length = new ArrayList<>();
        skipList.insert(new CItemset(new int[]{1, 2}, 10));
        skipList.insert(new CItemset(3));
        skipList.insert(new CItemset(2));
        skipList.insert(new CItemset(4));
        skipList.insert(new CItemset(7));
        skipList.insert(new CItemset(9));
        skipList.insert(new CItemset(1));
        skipList.insert(new CItemset(new int[]{1, 5}, 120));
        skipList.insert(new CItemset(new int[]{1, 3, 5}, 140));
        skipList.insert(new CItemset(5));
        skipList.insert(new CItemset(new int[]{1, 2, 4, 5}, 8));
//        System.out.println(skipList.findByLength(1));
//        skipList.printAll();
//        System.out.println(skipList.find(new CItemset(new int[]{1, 5}, 120)));
        skipList.delete(new CItemset(new int[]{1, 2, 4, 5}));
        skipList.delete(new CItemset(new int[]{1, 3, 5}));
        System.out.println(skipList.Nodecount);
        skipList.printAll();
    }*/
}