package scheduler2022.repository;

import scheduler2022.DynamicPCInfo;
import scheduler2022.util.DHTutil;

public class PCInfoRepository {
 public void saveDynamic(String ip, DynamicPCInfo dpi) {
     DHTutil.setPcInfo(ip, dpi);
 }
}