package ca.pfv.spmf.algorithms.frequentpatterns.AlgoFCHM_Stream;

import ca.pfv.spmf.tools.MemoryLogger;

import java.io.*;
import java.util.*;


public class AlgoFCHM_Stream {
    //debug模式,debug=true，显示一些debug信息
    boolean debug = false;
    //最大内存使用率
    public double maxMemory = 0;
    //算法开始时间
    public long startTimestamp = 0;
    //算法结束时间
    public long endTimestamp = 0;
    //计算算法内存占用
    public long startMem = 0, endMem = 0;
    //算法当前处理的事务数
    public int transactionCount;
    //项与TWU的映射
    Map<Integer, Integer> mapItemToTWU;
    //项与效用列表的映射
    private Map<Integer, CList> mapItemToUtilityList;
    //一项集效用列表的集合
    List<CList> listOfUtilityLists;
    ArrayList<CList> listULForRecursion;
    //列表的头节点，指示列表中所有项第一次出现的位置
    CList HeadNode;
    //生成的闭合高效用项集的数量
    public int ChuiCount = 0;
    //候选项集数量
    public int candidateCount = 0;
    //所有事务的效用
    int totalDBUtility = 0;
    //最大事务数
    int MaxTid = 0;
    BufferedWriter writer = null;
    //算法参数
    /**
     * number of batches that have been processed
     */
    int processedBatchCount;//记录处理的批次数量
    int oldestBratchCount;//记录窗口中的最旧批次
    public int minutil;//最小效用阈值
    public int win_size, batch_size, win_number;//窗口大小，批次大小，窗口号
    private String resultFile;
    static ArrayList<ArrayList<String>> window = new ArrayList<ArrayList<String>>();//窗口，字符串类型的二维array数组

    /*
     * EUCE策略
     * */
    boolean useEUCPstrategy;
    /**
     * Determine if the EUCP strategy will be used
     */
    Map<Integer, Map<Integer, Integer>> mapFMAP;  // PAIR OF ITEMS , item --> item, twu
    private boolean ENABLE_LA_PRUNE;
    boolean isFirstWindow;
    /**
     * 旧批次更新时间
     */
    public long oldBatchTime = 0, olfBatchtimeStamp = 0;
    long everyBatchTime = 0, everyBatchTimeStamp = 0;
    //        long time_Construct1 = 0, temp_Construct1 = 0;
    long time_Construct = 0, temp_Construct = 0;
    long time_NoBack = 0, temp_NoBack = 0;
    //    long time_NOJump = 0, temp_NOJump = 0;
//    long time_Jump = 0, temp_Jump = 0;
    long time_Test = 0, temp_Test = 0;
    int Bprune_num = 0;
    int Bprune_num1 = 0;
    int LAprune_num = 0;
    int construct_num = 0;
    int new_num = 0, update_num = 0, pattern_num = 0;
    //存放闭合结果集的闭合列表
    public ResultSet resultset = null;
    //设置结果集预估大小
    private int hashTableSize = 1200000;//设置一个较大的值就行
    private int OldBatchTid;//旧批次的Tid
    private List<Integer> TU = new ArrayList<Integer>();

    class Pair {
        int item = 0;
        int utility = 0;

        public String toString() {
            return "[" + item + "," + utility + "]";
        }
    }

    /**
     * Get the number of HUIs stored in the skiplist structure.
     *
     * @return the number of HUIs.
     */
    public int getRealCHUICount() {
        int count = 0;
        for (SkipList skipList : resultset.ClosedTable) {
            if (skipList == null) {
                continue;
            }
            count += skipList.getCount();
        }
        return count;
    }

    /**
     * Write CHUIs found to a file. Note that this method write all CHUIs found
     * until now and erase the file by doing so, if the file already exists.
     *
     * @param output the output file path
     * @throws IOException if error writing to output file
     */
    public int writeCHUIsToFile(String output) throws IOException {
        int patternCount = 0;
        for (SkipList skipList : resultset.ClosedTable) {
            if (skipList == null) {
                continue;
            }
            patternCount += skipList.getCount();
            skipList.writeToFile(writer);
        }
        ChuiCount += patternCount;
        writer.write("Closed High utility itemsets count: " + patternCount);
        writer.newLine();
        writer.write("////////////////////////BATCH" + processedBatchCount);
        writer.newLine();
        return patternCount;
//        System.out.println("该批次项集个数：" + patternCount);
    }

    //构造函数
    public AlgoFCHM_Stream(boolean useEUCPstrategy, boolean ENABLE_LA_PRUNE) {
        resultset = new ResultSet(hashTableSize);//初始化闭合结果集哈希表
        listOfUtilityLists = new LinkedList<CList>();
        mapItemToUtilityList = new HashMap<Integer, CList>();
        mapItemToTWU = new HashMap<Integer, Integer>();
        this.useEUCPstrategy = useEUCPstrategy;
        this.ENABLE_LA_PRUNE = ENABLE_LA_PRUNE;

        if (useEUCPstrategy) {
            mapFMAP = new HashMap<Integer, Map<Integer, Integer>>();
        }
    }

    public void runAlgorithm(String transactionFile, int minutil, int win_size,
                             int batch_size, String resultFile)
            throws IOException {
        Runtime r = Runtime.getRuntime();
        r.gc();//计算内存前先垃圾回收一次
        startTimestamp = System.currentTimeMillis();
        startMem = r.freeMemory(); // 开始Memory
        MemoryLogger.getInstance().reset();
        ChuiCount = 0;
        transactionCount = 0;
        processedBatchCount = 0;//记录当前最新批次
        oldestBratchCount = 0;//记录窗口中的最旧批次
        this.minutil = minutil;
        this.win_size = win_size;
        this.batch_size = batch_size;
        this.resultFile = resultFile;
        int flag = 1;//标识是否是第一个窗口
        //扫描数据库来创建窗口以及调用算法挖掘闭合项集
        // Scan the complete database to create windows and call FHM.
        BufferedReader myInput = null;
        String thisLine;
        int iterateBatch = 0, iterateWindow = 0;
        ArrayList<String> newBatchTransaction = new ArrayList<String>();//存储批次中的事务
//        ArrayList<String> oldestBatchTransaction = new ArrayList<String>();//存储批次中的事务

        try {
            if (writer == null) {
                writer = new BufferedWriter(new FileWriter(resultFile));
            }
            // prepare the object for reading the file
            myInput = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(transactionFile))));
            // for each line (transaction) until the end of file
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is a comment, is empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
                        || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                iterateBatch++;//批次中事务的数量
                transactionCount++;
                if (flag == 1) {//处理第一个窗口中的事务，因为之后的窗口需要滑动
                    //当前批次没有达到用户定义的一个批次中事务的数量，添加事务到批次当中
                    if (iterateBatch <= this.batch_size) {
                        newBatchTransaction.add(thisLine);
                    }
                    //当前批次已达到用户定义的一个批次中事务的数量，将批次添加到窗口当中
                    if ((iterateBatch == this.batch_size)) {
                        processedBatchCount++;//批次数++
                        iterateBatch = 0;//事务数归零
                        window.add(new ArrayList<String>(newBatchTransaction));//将当前批次添加到窗口中
                        newBatchTransaction.clear();
                        iterateWindow++;//窗口中批次数量++
                        if (iterateWindow >= this.win_size) {//窗口中批次数量大于窗口大小，窗口滑动
                            iterateWindow = 0;//批次数归零
                            everyBatchTimeStamp = System.currentTimeMillis();
                            miningProcessfirsttime(window);
                            everyBatchTime = System.currentTimeMillis() - everyBatchTimeStamp;
                            System.out.println("窗口" + processedBatchCount + "用时： " + everyBatchTime);
                            window.remove(0);//从窗口这个二维的字符数组上删除位置0上的批次，删除最旧的批次
                            flag = 0;//之后是之后窗口的处理
                        }
                    }
                } else if (flag == 0) {//之后每次挖掘新批次
                    if (iterateBatch <= this.batch_size) {
                        newBatchTransaction.add(thisLine);
                    }
                    if ((iterateBatch == this.batch_size)) {
                        processedBatchCount++;
                        OldBatchTid = transactionCount - (win_size + 1) * batch_size + 1;
                        iterateBatch = 0;
                        window.add(new ArrayList<String>(newBatchTransaction));//将当前批次添加到窗口中
//
                        everyBatchTimeStamp = System.currentTimeMillis();
                        new_num = 0;
                        update_num = 0;
                        processOldBatch(OldBatchTid);//处理闭合结果集中旧批次的项集
                        miningProcessnewbatch(newBatchTransaction, transactionCount - batch_size + 1);//插入新批次后，更新结果集
                        everyBatchTime = System.currentTimeMillis() - everyBatchTimeStamp;
                        System.out.println("窗口" + processedBatchCount + "用时： " + everyBatchTime);
                        System.out.println("总数:" + pattern_num + "添加数：" + new_num + "更新数:" + update_num + "未更新数:" + (pattern_num - new_num - update_num));
                        System.out.println("更新占比" + String.format("%.2f", ((double) (update_num) / (double) pattern_num * 100)) + "未更新数占比：" + String.format("%.2f", ((double) (pattern_num - new_num - update_num) / (double) pattern_num * 100)));
                        window.remove(0);//从窗口这个二维的字符数组上删除位置0上的批次，删除最旧的批次
                        newBatchTransaction.clear();
                    }
                }
            }
            //两种异常情况，1、若事务数没有达到用户设定的批次中的数量，则批次数不会增加。2、若批次数没有达到窗口大小，则不会挖掘。
            // if it is last batch with elements less than user specified batch
            // elements
            if ((iterateBatch > 0) && (iterateBatch < this.batch_size)) {
                if (flag == 1) {
                    processedBatchCount++;
                    window.add(new ArrayList<String>(newBatchTransaction));//将当前批次添加到窗口中
                    miningProcessfirsttime(window);
                    newBatchTransaction.clear();
                } else {
                    processedBatchCount++;
                    if (isFirstWindow)
                        OldBatchTid = 1;
                    else
                        OldBatchTid = OldBatchTid + batch_size;
                    transactionCount = (processedBatchCount - 1) * batch_size + 1;//当前处理的最新批次的第一个事务的事务Tid
                    everyBatchTimeStamp = System.currentTimeMillis();
                    new_num = 0;
                    update_num = 0;
                    processOldBatch(OldBatchTid);//处理闭合结果集中旧批次的项集
                    miningProcessnewbatch(newBatchTransaction, transactionCount);//插入新批次后，更新结果集
                    everyBatchTime = System.currentTimeMillis() - everyBatchTimeStamp;
                    System.out.println("窗口" + processedBatchCount + "用时： " + everyBatchTime);
                    System.out.println("总数:" + pattern_num + "添加数：" + new_num + "更新数:" + update_num + "未更新数:" + (pattern_num - new_num - update_num));
                    System.out.println("更新占比" + String.format("%.2f", ((double) (update_num) / (double) pattern_num * 100)) + "未更新数占比：" + String.format("%.2f", ((double) (pattern_num - new_num - update_num) / (double) pattern_num * 100)));
                    newBatchTransaction.clear();
                }
            }
        } catch (Exception e) {
            // catches exception if error while reading the input file
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
                writer.close();
            }
            newBatchTransaction.clear();
            window.clear();
        }
        endTimestamp = System.currentTimeMillis();
        endMem = r.freeMemory(); // 末尾Memory
    }

    //删除旧批次更新策略
    public void processOldBatch(int OldBatchTid) {
        //更新所有项的TWU
        //删除效用列表中最旧批次所对应的tid及其元组
        for (CList ulist : listOfUtilityLists) {
            for (int i = 0; i < batch_size; i++) {
                if (ulist.ifcontaintid(OldBatchTid + i) == false)
                    continue;
                else {
                    ulist.removetid(OldBatchTid + i);
                    mapItemToTWU.put(ulist.item, mapItemToTWU.get(ulist.item) - TU.get(OldBatchTid + i - 1));
                }
            }
        }

        //遍历存放结果集的闭合哈希列表，将其中删除旧批次后不再闭合以及不再高效用的项集删除
        for (SkipList skipList : resultset.ClosedTable) {
            if (skipList == null) {
                continue;
            }
            //删除结果集中项集在旧批次中的效用
            skipList.removeOldBatchUtility(OldBatchTid, batch_size, minutil);
        }
        //现在问题是，删除有些项集后，其支持度降低，但是在闭合结果集中还处于不同的支持度，无法判断。
        //判断项集在结果集当中是否闭合
        //遍历结果集中有标记的项集，检查与这些项集支持度相同的列表中检查是否有其超集，有的话则删除这些项集
        //若没有的话，将该有标记的项集加入到对应支持度的列表当中，并取消该标记
        for (SkipList skipList : resultset.ClosedTable) {
            if (skipList == null) {
                continue;
            }
            skipList.updateSkipList(resultset.ClosedTable);
        }
    }

    //之后批次到达，仅处理最新批次
    /*
     * para newBatchTransaction 当前处理的最新批次
     * para currentTid   当前处理的最新批次的第一个事务的Tid
     *
     * */
    public void miningProcessnewbatch(ArrayList<String> newBatchTransaction, int currentTid) throws IOException {
        isFirstWindow = false;
        //按批次读取窗口中的每个批次
        int tid = currentTid - 1;
        //读取当前最新批次中的内容
        for (String thisLine : newBatchTransaction) {//读取批次中的每个事务
            if (thisLine.isEmpty() == true
                    || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                continue;
            }
            // split the transaction according to the : separator
            String split[] = thisLine.split(":");
            // the first part is the list of items
            String items[] = split[0].split(" ");
            // the second part is the transaction utility
            int transactionUtility = Integer.parseInt(split[1]);
            // the third part is the items' utilies
            String utilityValues[] = split[2].split(" ");
            tid++;
            //在TU数组中保存事务的效用
            TU.add(transactionUtility);
            // for each item, we add the transaction utility to its TWU
            for (int i = 0; i < items.length; i++) {
                // convert item to integer
                Integer item = Integer.parseInt(items[i]);
                // get the current TWU of that item
                Integer twu = mapItemToTWU.get(item);
                // add the utility of the item in the current transaction to its twu
                CList uList;
                if (twu == null) {
                    uList = new CList(item);
                    mapItemToUtilityList.put(item, uList);
                    listOfUtilityLists.add(uList);
                    twu = transactionUtility;
                } else {
                    twu = twu + transactionUtility;
                    uList = mapItemToUtilityList.get(item);
                }
                mapItemToTWU.put(item, twu);
                // get the utility list of this item
                uList.addElementN(new CElement(tid, Integer.parseInt(utilityValues[i]), 0));
            }
            totalDBUtility += transactionUtility;
        }

        // Sort the items by TWU
        Collections.sort(listOfUtilityLists, new Comparator<CList>() {
            public int compare(CList o1, CList o2) {
                // compare the TWU of the items
                return compareItems(o1.item, o2.item);
            }
        });
        MaxTid = tid;
        //update the ru of items appear in D's transaction according to the new TWU order
        int[] TA = new int[MaxTid + 1];//设置为事务数量大小
        Arrays.fill(TA, 0);
        //get the ul of item having largest TWU, then initialize the temp utility array TA
        CList uli = (CList) listOfUtilityLists.get(listOfUtilityLists.size() - 1);
        for (CElement ele : uli.elementsC) {
            ele.rutils = 0;
            TA[ele.tid] = ele.iutils;
        }

        uli.sumRutilsC = 0;
        uli.sumRutilsN = 0;
        for (CElement ele : uli.elementsN) {
            ele.rutils = 0;
            TA[ele.tid] = ele.iutils;
        }

        //re-calculate ru of smaller items
        ListIterator li = listOfUtilityLists.listIterator(listOfUtilityLists.size() - 1);
        while (li.hasPrevious()) {
            CList ul = (CList) li.previous();
            ul.sumRutilsC = 0;
            for (CElement ele : ul.elementsC) {
                ele.rutils = TA[ele.tid];
                ul.sumRutilsC += TA[ele.tid];
                TA[ele.tid] += ele.iutils;
            }
            ul.sumRutilsN = 0;
            for (CElement ele : ul.elementsN) {
                ele.rutils = TA[ele.tid];
                ul.sumRutilsN += TA[ele.tid];
                TA[ele.tid] += ele.iutils;
            }
        }

        TA = null;
        //Runtime.getRuntime().gc();
//        // Remove itemsets of size 1 that do not appear in DP
        listULForRecursion = new ArrayList<CList>();
        for (CList temp : listOfUtilityLists) {
            // we keep only utility lists of items in DP temp.sumIutilsDP != 0 &&
            if (mapItemToTWU.get(temp.item) >= minutil && temp.sumIutilsN != 0) {
                listULForRecursion.add(temp);
            }
        }
        // Mine the database recursively
        FCHM_Stream(true, new int[0], null, new ArrayList<CList>(), listULForRecursion);
        pattern_num = writeCHUIsToFile(resultFile);
        //将之前结果产生的效用列表并入公共效用列表当中
        for (CList ulist : listOfUtilityLists) {
            ulist.switchNtoC();
        }
        // check the memory usage again and close the file.
//        checkMemory();
    }

    //处理第一个窗口
    public void miningProcessfirsttime(ArrayList<ArrayList<String>> window) throws IOException {
//        HeadNode = new CList(-1);
//        temp_Construct1 = System.currentTimeMillis();
        isFirstWindow = true;
        //按批次读取窗口中的每个批次
        int tid = 0;
        //读取当前窗口中的内容
        for (ArrayList<String> batch_transactions : window) {
            for (String thisLine : batch_transactions) {//读取批次中的每个事务

                if (thisLine.isEmpty() == true
                        || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                    continue;
                }
                tid++;
                // split the transaction according to the : separator
                String split[] = thisLine.split(":");
                // the first part is the list of items
                String items[] = split[0].split(" ");
                // the second part is the transaction utility
                int transactionUtility = Integer.parseInt(split[1]);
                // the third part is the items' utilies
                String utilityValues[] = split[2].split(" ");
                //在TU数组中保存事务的效用
                TU.add(transactionUtility);
                // for each item, we add the transaction utility to its TWU
                for (int i = 0; i < items.length; i++) {
                    // convert item to integer
                    Integer item = Integer.parseInt(items[i]);
                    // get the current TWU of that item
                    Integer twu = mapItemToTWU.get(item);
                    // add the utility of the item in the current transaction to its twu
                    CList uList;
                    if (twu == null) {
                        uList = new CList(item);
                        mapItemToUtilityList.put(item, uList);
                        listOfUtilityLists.add(uList);
                        twu = transactionUtility;
                    } else {
                        twu = twu + transactionUtility;
                        uList = mapItemToUtilityList.get(item);
                    }
                    mapItemToTWU.put(item, twu);
                    // get the utility list of this item
                    uList.addElementN(new CElement(tid, Integer.parseInt(utilityValues[i]), 0));
                }
                totalDBUtility += transactionUtility;
            }
        }
        // Sort the items by TWU
        Collections.sort(listOfUtilityLists, new Comparator<CList>() {
            public int compare(CList o1, CList o2) {
                // compare the TWU of the items
                return compareItems(o1.item, o2.item);
            }
        });
//        // Remove itemsets of size 1 that do not appear in DP
        listULForRecursion = new ArrayList<CList>();
        for (CList temp : listOfUtilityLists) {
            // we keep only utility lists of items in DP temp.sumIutilsDP != 0 &&
            if (mapItemToTWU.get(temp.item) >= minutil && temp.sumIutilsN != 0) {
                listULForRecursion.add(temp);
            }
        }
        //update the ru of items appear in D's transaction according to the new TWU order
        MaxTid = tid;
        int[] TA = new int[MaxTid + 1];//设置为事务数量大小

        Arrays.fill(TA, 0);
        //get the ul of item having largest TWU, then initialize the temp utility array TA
        CList uli = (CList) listULForRecursion.get(listULForRecursion.size() - 1);
        for (CElement ele : uli.elementsC) {
            ele.rutils = 0;
            TA[ele.tid] = ele.iutils;
        }
        uli.sumRutilsC = 0;

        for (CElement ele : uli.elementsN) {
            ele.rutils = 0;
            TA[ele.tid] = ele.iutils;
        }
        uli.sumRutilsN = 0;
        //re-calculate ru of smaller items
        ListIterator li = listULForRecursion.listIterator(listULForRecursion.size() - 1);
        while (li.hasPrevious()) {
            CList ul = (CList) li.previous();
            for (CElement ele : ul.elementsC) {
                ele.rutils = TA[ele.tid];
                ul.sumRutilsC += TA[ele.tid];
                TA[ele.tid] += ele.iutils;
            }

            for (CElement ele : ul.elementsN) {
                ele.rutils = TA[ele.tid];
                ul.sumRutilsN += TA[ele.tid];
                TA[ele.tid] += ele.iutils;
            }
        }
        TA = null;
//        time_Construct1 += System.currentTimeMillis() - temp_Construct1;
        ///////////////////////////////////////////////////////////////
        //读取当前窗口中的内容
        if (useEUCPstrategy) {
            for (ArrayList<String> batch_transactions : window) {
                for (String thisLine : batch_transactions) {//读取批次中的每个事务

                    if (thisLine.isEmpty() == true
                            || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%' || thisLine.charAt(0) == '@') {
                        continue;
                    }
                    // split the transaction according to the : separator
                    String split[] = thisLine.split(":");
                    // the first part is the list of items
                    String items[] = split[0].split(" ");
                    // the second part is the transaction utility
                    int transactionUtility = Integer.parseInt(split[1]);
                    // the third part is the items' utilies
                    String utilityValues[] = split[2].split(" ");
                    int newTU = 0;
                    // Create a list to store items
                    List<Pair> revisedTransaction = new ArrayList<Pair>();
                    // for each item
                    for (int i = 0; i < items.length; i++) {
                        /// convert values to integers
                        Pair pair = new Pair();
                        pair.item = Integer.parseInt(items[i]);
                        pair.utility = Integer.parseInt(utilityValues[i]);
                        // if the item has enough utility
                        if (mapItemToTWU.get(pair.item) >= minutil) {
                            // add it
                            revisedTransaction.add(pair);
                            newTU += pair.utility; // NEW OPTIMIZATION
                        }
                    }
                    // sort the transaction
                    Collections.sort(revisedTransaction, new Comparator<Pair>() {
                        public int compare(Pair o1, Pair o2) {
                            return compareItems(o1.item, o2.item);
                        }
                    });
                    // for each item left in the transaction
                    for (int i = 0; i < revisedTransaction.size(); i++) {
                        Pair pair = revisedTransaction.get(i);
                        // BEGIN CODE for updating the structure used
                        // BY THE EUCP STRATEGY INTRODUCED IN CHUIMiner
                        if (useEUCPstrategy) {
                            Map<Integer, Integer> mapFMAPItem = mapFMAP.get(pair.item);
                            if (mapFMAPItem == null) {
                                mapFMAPItem = new HashMap<Integer, Integer>();
                                mapFMAP.put(pair.item, mapFMAPItem);
                            }

                            for (int j = i + 1; j < revisedTransaction.size(); j++) {
                                Pair pairAfter = revisedTransaction.get(j);
                                Integer twuSum = mapFMAPItem.get(pairAfter.item);
                                if (twuSum == null) {
                                    mapFMAPItem.put(pairAfter.item, newTU);
                                } else {
                                    mapFMAPItem.put(pairAfter.item, twuSum + newTU);
                                }
                            }
                        }
                        // END OF CODE FOR EUCP STRATEGY
                    }
                }
            }
        }
        ///////////////////////////////////////////////////////////////
        // Mine the database recursively
        FCHM_Stream(true, new int[0], null, new ArrayList<CList>(), listULForRecursion);

        writeCHUIsToFile(resultFile);
        //将之前结果产生的效用列表并入公共效用列表当中
        for (CList ulist : listOfUtilityLists) {
            ulist.switchNtoC();
        }
        // check the memory usage again and close the file.
        MemoryLogger.getInstance().checkMemory();
    }

    /**
     * 更新所有结果集当中效用以及支持度需要变化的项集
     *
     * @param newBatchClistSet
     */
    private void updateResultSet(ArrayList<CList> newBatchClistSet) {
        for (SkipList skipList : resultset.ClosedTable) {
            if (skipList == null) {
                continue;
            }
//            skipList.removeOldBatchUtility(OldBatchTid, batch_size, minutil);
        }
    }

    private void FCHM_Stream(boolean firstTime, int[] closedSet, CList closedSetUL,
                             List<CList> preset, List<CList> postset)
            throws IOException {
        for (CList iUL : postset) {
            CList newgen_TIDs;
            if (firstTime) {
                newgen_TIDs = iUL;
            } else {
                newgen_TIDs = construct1(closedSetUL, iUL);
            }
            if (isPassingHUIPruning(newgen_TIDs)) {
                int[] newGen = appendItem(closedSet, iUL.item);
                if (is_dup(newgen_TIDs, preset) == false) {
                    int[] closedSetNew = newGen;
                    CList closedsetNewTIDs = newgen_TIDs;
                    List<CList> postsetNew = new ArrayList<CList>();

                    boolean passedHUIPruning = true;
                    for (CList jUL : postset) {
                        if (jUL.item == (iUL.item) || compareItems(jUL.item, iUL.item) < 0) {
                            continue;
                        }
                        // If J does not appear in DP, then we can skip it
                        if (jUL.sumIutilsN == 0) {
                            continue;
                        }
                        boolean shouldPrune = isFirstWindow && useEUCPstrategy && checkEUCPStrategy(iUL.item, jUL.item);
                        if (shouldPrune) {
                            continue;
                        }
                        if (containsAllTIDS1(jUL, newgen_TIDs)) {
                            closedsetNewTIDs = construct1(closedsetNewTIDs, jUL);
                            closedSetNew = appendItem(closedSetNew, jUL.item);
                            if (isPassingHUIPruning(closedsetNewTIDs) == false) {
                                passedHUIPruning = false;
                                break;
                            }
                        } else {
                            postsetNew.add(jUL);
                        }
                    }
                    if (passedHUIPruning) {
                        // L15 : write out Closed_setNew and its support
                        if (closedsetNewTIDs.sumIutilsC + closedsetNewTIDs.sumIutilsN >= minutil) {
                            writeOut(closedSetNew, closedsetNewTIDs, closedsetNewTIDs.getSupport(), closedsetNewTIDs.sumIutilsC + closedsetNewTIDs.sumIutilsN);
                        }
                        List<CList> presetNew = new ArrayList<CList>(preset);
                        FCHM_Stream(false, closedSetNew, closedsetNewTIDs, presetNew, postsetNew);
                    }
                }
                preset.add(iUL);
            }
        }
        candidateCount++;
        MemoryLogger.getInstance().checkMemory();
    }

    /**
     * The method "is_dup" as described in the paper.
     *
     * @param newgenTIDs the tidset of newgen
     * @param preset     the itemset "preset"
     */
    private boolean is_dup(CList newgenTIDs, List<CList> preset) {
        // L25
        // for each integer j in preset
        for (CList j : preset) {
            // for each element in the utility list of pX
            boolean containsAllinC = true;
            boolean containsAllinN = true;
            if (newgenTIDs.getSupport() > j.getSupport())
                continue;
            for (CElement elmX : newgenTIDs.elementsN) {
                // do a binary search to find element ey in py with tid = ex.tid
                CElement elmE = findElementWithTID(j.elementsN, elmX.tid);
                if (elmE == null) {
                    containsAllinN = false;
                    break;
                }
            }
            if (containsAllinN != false && newgenTIDs.elementsC.size() != 0)
                for (CElement elmX : newgenTIDs.elementsC) {
                    // do a binary search to find element ey in py with tid = ex.tid
                    CElement elmE = findElementWithTID(j.elementsC, elmX.tid);
                    if (elmE == null) {
                        containsAllinC = false;
                        break;
                    }
                }
            // L26 :
            // If tidset of newgen is included in tids of j, return true
            if (containsAllinC && containsAllinN) {
                // IMPORTANT
                // NOTE THAT IN ORIGINAL PAPER THEY WROTE FALSE, BUT IT SHOULD BE TRUE
                return true;
            }
        }
        return false;  // NOTE THAT IN ORIGINAL PAPER THEY WROTE TRUE, BUT IT SHOULD BE FALSE
    }


    /**
     * Do a binary search to find the element with a given tid in a utility list
     *
     * @param list the utility list
     * @param tid  the tid
     * @return the element or null if none has the tid.
     */
    private CElement findElementWithTID(List<CElement> list, int tid) {
        // perform a binary search to check if  the subset appears in  level k-1.
        int first = 0;
        int last = list.size() - 1;
        if (first <= last && (tid > list.get(last).tid || tid < list.get(first).tid))
            return null;
        // the binary search
        while (first <= last) {
            int middle = (first + last) >>> 1; // divide by 2

            if (list.get(middle).tid < tid) {
                first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
            } else if (list.get(middle).tid > tid) {
                last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
            } else {
                return list.get(middle);
            }
        }
        return null;
    }


    /**
     * This method performs the EUCP pruning from the FHM algorithm (see FHM paper at ISMIS 2014)
     *
     * @param itemX an item X
     * @param itemY an item Y
     * @return true if  TWU({x,y} < minutil.  Otherwise return false
     */
    private boolean checkEUCPStrategy(int itemX, int itemY) {
        Map<Integer, Integer> mapTWUF = mapFMAP.get(itemX);
        if (mapTWUF != null) {
            Integer twuF = mapTWUF.get(itemY);
            if (twuF == null || twuF < minutil) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param X         Candidate
     * @param support   support
     * @param resultset set of closed HUIs
     *                  在结果集中查找是否有已挖掘出来的闭合项集是当前项集的支持度相同的超集
     *                  (1 the length of the CHUI is greater than |X|
     *                  (2 its support is equal to SUP(X)
     * @return true if P U {e} has no backward extension
     */
    boolean HasNoBackwardExtension(int[] X, int support, ResultSet resultset) {
        int length = X.length;
        SkipList skipList = resultset.ClosedTable[support];//获取支持度为support的跳表
        if (skipList == null)
            return true;
        int n = skipList.getTailCItemset().getlength();//获取当前CHUIs中的闭合项集的最大长度
        if (length >= n)//项集X的长度是否大于CHUIs中最长项集的长度
            return true;
//        temp_Test = System.currentTimeMillis();
        for (int i = length + 1; i <= n; i++)
            for (CItemset citemset : skipList.findByLength(i)) {
                if (citemset.contains(new CItemset(X)))
                    return false;
            }
//        time_Test += System.currentTimeMillis() - temp_Test;
        return true;
    }

    private CList construct1(CList uX, CList uE) {

        // create an empy utility list for pXY
        CList uXE = new CList(uE.item);

        //== new optimization - LA-prune  == /
        // Initialize the sum of total utility
        long totalUtility = uX.sumIutilsN + uX.sumRutilsN + uX.sumIutilsC + uX.sumRutilsC;

        for (CElement ex : uX.elementsN) {
//            // do a binary search to find element ey in py with tid = ex.tid
            CElement ey = findElementWithTID(uE.elementsN, ex.tid);
            if (ey == null) {
                //== new optimization - LA-prune == /
                if (ENABLE_LA_PRUNE) {
                    totalUtility -= (ex.iutils + ex.rutils);
                    if (totalUtility < minutil) {
                        LAprune_num++;
                        return null;
                    }
                }
                // =============================================== /
                continue;
            }
            // Create new element
            CElement eXY = new CElement(ex.tid, ex.iutils + ey.iutils, ex.rutils - ey.iutils);
            // add the new element to the utility list of pXY
            uXE.addElementN(eXY);
        }
        // PRUNING: IF THERE IS NO ELEMENT IN DP, WE DON'T NEED TO CONTINUE
        if (uXE.elementsN.isEmpty()) {
            return null;
        }

        // for each element in the utility list of pX
        for (CElement elmX : uX.elementsC) {
            // do a binary search to find element ey in py with tid = ex.tid
            CElement elmE = findElementWithTID(uE.elementsC, elmX.tid);
            if (elmE == null) {
                //== new optimization - LA-prune == /
                if (ENABLE_LA_PRUNE) {
                    totalUtility -= (elmX.iutils + elmX.rutils);
                    if (totalUtility < minutil) {
                        LAprune_num++;
                        return null;
                    }
                }
                // =============================================== /
                continue;
            }
            // Create the new element
            CElement elmXe = new CElement(elmX.tid, elmX.iutils + elmE.iutils, elmX.rutils - elmE.iutils);
            // add the new element to the utility list of pXY
            uXE.addElementC(elmXe);
        }
        construct_num++;
        return uXE;
    }

    /**
     * 在构建项集时，检查该项集是否已存在于结果集当中，若存在将N部分加入，并更新，若不存在则构建并插入
     *
     * @param uX
     * @param uE
     * @return
     */
    private CList construct2(int[] itemset, CList uX, CList uE) {

        // create an empy utility list for pXY
        CList uXE = new CList(uE.item);

        //== new optimization - LA-prune  == /
        // Initialize the sum of total utility
        long totalUtility = uX.sumIutilsN + uX.sumRutilsN + uX.sumIutilsC + uX.sumRutilsC;
        for (CElement ex : uX.elementsN) {
//            // do a binary search to find element ey in py with tid = ex.tid
            CElement ey = findElementWithTID(uE.elementsN, ex.tid);
            if (ey == null) {
                //== new optimization - LA-prune == /
                if (ENABLE_LA_PRUNE) {
                    totalUtility -= (ex.iutils + ex.rutils);
                    if (totalUtility < minutil) {
                        LAprune_num++;
                        return null;
                    }
                }
                // =============================================== /
                continue;
            }
            // Create new element
            CElement eXY = new CElement(ex.tid, ex.iutils + ey.iutils, ex.rutils - ey.iutils);
            // add the new element to the utility list of pXY
            uXE.addElementN(eXY);
        }
        // PRUNING: IF THERE IS NO ELEMENT IN DP, WE DON'T NEED TO CONTINUE
        if (uXE.elementsN.isEmpty()) {
            return null;
        }
        if (!isFirstWindow) {
            int CountX_C = uX.elementsC.size();
//        int CountX_N = uX.elementsN.size();
            int CountY_C = uE.elementsC.size();
//        int CountY_N = uE.elementsN.size();
            int supportTofind = 0;
            int posX = 0, posY = 0;
            while (posX < CountX_C && posY < CountY_C) {
                CElement ex = uX.elementsC.get(posX);
                CElement ey = uE.elementsC.get(posY);
                if (ex.tid < ey.tid) {
                    posX++;
                } else if (ex.tid > ey.tid) {
                    posY++;
                } else {
                    supportTofind++;
                    posX++;
                    posY++;
                }
            }
            CItemset itemsetRetrieved = resultset.retrieveItemset(appendItem(itemset, uE.item), supportTofind);
            if (itemsetRetrieved == null) {
                //put new
                // for each element in the utility list of pX
                for (CElement elmX : uX.elementsC) {
                    // do a binary search to find element ey in py with tid = ex.tid
                    CElement elmE = findElementWithTID(uE.elementsC, elmX.tid);
                    if (elmE == null) {
                        //== new optimization - LA-prune == /
                        if (ENABLE_LA_PRUNE) {
                            totalUtility -= (elmX.iutils + elmX.rutils);
                            if (totalUtility < minutil) {
                                LAprune_num++;
                                return null;
                            }
                        }
                        // =============================================== /
                        continue;
                    }
                    // Create the new element
                    CElement elmXe = new CElement(elmX.tid, elmX.iutils + elmE.iutils, elmX.rutils - elmE.iutils);
                    // add the new element to the utility list of pXY
                    uXE.addElementC(elmXe);
                }
            } else {
                //update
                resultset.remove(itemsetRetrieved, supportTofind);//删除结果集中其所在错误的支持度对应的HashSet
                itemsetRetrieved = itemsetRetrieved.updateCitemset(uXE);
                resultset.put(itemsetRetrieved, itemsetRetrieved.getSupport());
                update_num++;
            }
        }
//        construct_num++;
        return uXE;
    }


    /**
     * Check if a utility list contains all tids from another utility list
     *
     * @param ul1 the first utility list
     * @param ul2 the second utility list
     * @return true if it contains all tids, otherwise false.
     */
    private boolean containsAllTIDS1(CList ul1, CList ul2) {
        // for each integer j in preset

        for (CElement elmX : ul2.elementsN) {
            // do a binary search to find element ey in py with tid = ex.tid
            CElement elmE = findElementWithTID(ul1.elementsN, elmX.tid);
            if (elmE == null) {
                return false;
            }
        }
        for (CElement elmX : ul2.elementsC) {
            // do a binary search to find element ey in py with tid = ex.tid
            CElement elmE = findElementWithTID(ul1.elementsC, elmX.tid);
            if (elmE == null) {
                return false;
            }
        }

        return true;
    }


    /**
     * 将挖掘出来的闭合项集写入结果集当中
     *
     * @param itemset
     * @param clist
     * @param support
     * @param utility
     */
/*    private void writeOut(int[] itemset, CList clist, int support, int utility) {
        //put new
        CItemset itemsetRetrieved = new CItemset(itemset, utility, support, clist);
        resultset.put(itemsetRetrieved, itemsetRetrieved.support);
        new_num++;
    }*/
    private void writeOut(int[] itemset, CList clist, int support, int utility) {
        if (utility >= minutil) {
            int supportTofind = 0;
            CItemset itemsetRetrieved;
            if (clist.elementsC.size() == 0)
                supportTofind = clist.elementsN.size();
            else
                supportTofind = clist.elementsC.size();
            if (isFirstWindow == true)
                itemsetRetrieved = null;//第一个窗口中挖掘出来的项集都未存入结果集，所以无需在结果集中检索
            else
                itemsetRetrieved = resultset.retrieveItemset(itemset, supportTofind);//之后窗口需要在结果集中检索是否存在
            if (itemsetRetrieved == null) {
                //put new
                itemsetRetrieved = new CItemset(itemset, utility, support, clist);
                resultset.put(itemsetRetrieved, itemsetRetrieved.support);
                new_num++;
            } else {
                //update
                resultset.remove(itemsetRetrieved, supportTofind);//删除结果集中其所在错误的支持度对应的HashSet
//                itemsetRetrieved = new CItemset(itemset, utility, support, clist);
                itemsetRetrieved = itemsetRetrieved.updateCitemset(clist);
                resultset.put(itemsetRetrieved, support);
                update_num++;
            }
        }
    }

    /**
     * Check the HUI pruning condition of HUI-Miner for the utilitylist of an itemset
     *
     * @param utilitylist the utility list of an itemset
     * @return true if it passes the pruning condition. Otherwise false.
     */
    private boolean isPassingHUIPruning(CList utilitylist) {
        return (utilitylist != null) && ((utilitylist.sumIutilsC + utilitylist.sumIutilsN + utilitylist.sumRutilsC + utilitylist.sumRutilsN) >= minutil);
    }

    /**
     * 仅检查列表在N部分的效用加剩余效用的加和
     *
     * @param utilitylist
     * @return
     */
    private boolean isPassingHUIPruning_N(CList utilitylist) {
        return (utilitylist != null) && ((utilitylist.sumIutilsN + utilitylist.sumRutilsN) >= minutil);
    }

    /**
     * Append an item to an itemset
     *
     * @param itemset an itemset represented as an array of integers
     * @param item    the item to be appended
     * @return the resulting itemset as an array of integers
     */
    private int[] appendItem(int[] itemset, int item) {
        int[] newgen = new int[itemset.length + 1];
        System.arraycopy(itemset, 0, newgen, 0, itemset.length);
        newgen[itemset.length] = item;
        return newgen;
    }


    /**
     * Method to compare items by their TWU
     *
     * @param item1 an item
     * @param item2 another item
     * @return 0 if the same item, >0 if item1 is larger than item2, <0
     * otherwise
     */
    private int compareItems(int item1, int item2) {
        //int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
        int value = Integer.compare(mapItemToTWU.get(item1), mapItemToTWU.get(item2));
//        int value = Integer.compare(item1,item2);//按照数字大小，从小到大排序
        // if the same, use the lexical order otherwise use the TWU
        //return (compare == 0) ? item1 - item2 : compare;
        if (value != 0) {
            return value;
        } else {
            return item1 - item2;
        }
    }


    /**
     * Print statistics about the latest execution to System.out.
     *
     * @throws IOException
     */
    public void printStats() throws IOException {

        System.out.println("=============  AlgoFCHM_Stream ALGORITHM v.220210 Stats =============");
        System.out.println(" minUtil = " + minutil);
        System.out.println(" Closed High utility itemsets count: " + ChuiCount);
        System.out.println(" Total time ~: " + (endTimestamp - startTimestamp) + " ms");
//        System.out.println(" oldBatchTime time ~: " + oldBatchTime + " ms");
        System.out.println(" Processed batch count: " + processedBatchCount);
        System.out.println(" Max memory:" + MemoryLogger.getInstance().getMaxMemory() + "MB");
        System.out.println(" CandidateCount:" + candidateCount);
        System.out.println(" LAprune_num : " + LAprune_num);
        System.out.println(" construct_num : " + construct_num);
//        System.out.println("新增率" + (float)new_num / pattern_num + "更新率" + (float)update_num / pattern_num + "不变率" + (float)(pattern_num - new_num - update_num) / pattern_num);
        System.out.println("=====================================");
    }
}
