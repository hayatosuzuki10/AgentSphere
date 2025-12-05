package primula.api.core.assh.command.interim;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

import primula.api.core.assh.command.AbstractCommand;

public class memory extends AbstractCommand {

	@Override
	public List<Object> runCommand(List<String> fileNames, Object instance, List<String> opt) {
		 //Java仮想マシンのメモリ管理システムにアクセスするための
        //オブジェクトを取得する
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();

        //ヒープメモリの使用量の情報を持つオブジェクトを取得する
        MemoryUsage heap = mbean.getHeapMemoryUsage();

        //VMが起動時にOSに要求するメモリ容量
        System.out.println("heap init:" + heap.getInit());
        //現在の使用量
        System.out.println("heap used:" + heap.getUsed());
        //使用できる状態になっているメモリ空間の容量
        System.out.println("heap committed:" + heap.getCommitted());
        ////VMが利用できるメモリ容量の最大値
        System.out.println("heap max:" + heap.getMax());

        //上記4つを同時に標準出力に表示する
        System.out.println(heap);

        //ヒープ以外のメモリの使用量の情報を持つオブジェクトを取得する
        MemoryUsage nonHeap = mbean.getNonHeapMemoryUsage();
        //VMが起動時にOSに要求するメモリ容量
        System.out.println("nonHeap init:" + nonHeap.getInit());
        //現在の使用量
        System.out.println("nonHeap used:" + nonHeap.getUsed());
        //使用できる状態になっているメモリ空間の容量
        System.out.println("nonHeap committed:" + nonHeap.getCommitted());
        //VMが利用できるメモリ容量の最大値
        System.out.println("nonHeap max:" + nonHeap.getMax());

        //上記4つを同時に標準出力に表示する
        System.out.println(nonHeap);
		return null;
	}

}
