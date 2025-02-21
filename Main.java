import Automation.BDaq.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Advantech_Module extends Thread {
    private int trig = 0;
    private int prev = 10;
    private int curr = 0;
    private boolean stop = false;
    private final BlockingQueue<Integer> commQ;

    public Advantech_Module(BlockingQueue<Integer> queue) {
        this.commQ = queue;
    }

    @Override
    public void run() {
        ErrorCode ret = ErrorCode.Success;
        EventCounterCtrl eventCounterCtrl = new EventCounterCtrl();
        InstantDiCtrl eventInputCtrl = new InstantDiCtrl();

        try {
            try {
                eventCounterCtrl.setSelectedDevice(new DeviceInformation("PCIE-1884,BID#0"));
            } catch (DaqException e) {
                throw new RuntimeException(e);
            }
            eventInputCtrl.setSelectedDevice(new DeviceInformation("PCIE-1884,BID#0"));

            eventCounterCtrl.setChannelStart(0);
            eventCounterCtrl.setChannelCount(1);
            eventCounterCtrl.setEnabled(true);

            while (!stop) {
                int[] data1 = new int[1];
                byte data2;
                ByteByRef data2_byte = new ByteByRef();
//                ErrorCode ret1 = eventCounterCtrl.Read(0, 1, data1);
//                ErrorCode ret2 = eventInputCtrl.ReadAny(0, 1, data2);

                ErrorCode ret1 = eventCounterCtrl.Read(0, data1);
                ErrorCode ret2 = eventInputCtrl.Read(0, data2_byte);
                data2 = data2_byte.value;

                if (BioFailed(ret1) || BioFailed(ret2)) {
                    break;
                }

                if (prev == 10 && data2 == 11) {
                    trig++;
                    prev = 11;
                    System.out.println("Trigger Count : " + trig);
                } else if (prev == 11 && data2 == 10) {
                    prev = 10;
                }

                if (data2 == 15) {
                    trig = 0;
                    System.out.println("Trigger Init : " + trig);
                }
                //

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            eventCounterCtrl.setEnabled(false);

        } catch (DaqException e) {
            throw new RuntimeException(e);
        } finally {
            eventCounterCtrl.Cleanup();
            eventInputCtrl.Cleanup();
        }

        if (BioFailed(ret)) {
            System.out.printf("Some error occurred, And the last error code is %#x\n", ret.toInt());
        }
    }


    private boolean BioFailed(Object ret) {
        int value;

        // ret이 ErrorCode 타입이면 value 가져오기
        if (ret instanceof ErrorCode) {
            value = ((ErrorCode) ret).toInt();
        } else if (ret instanceof Integer) {
            value = (int) ret;
        } else {
            throw new IllegalArgumentException("지원되지 않는 타입입니다.");
        }

        // 32비트 unsigned 비교 (Java에서는 int가 signed이므로 long으로 변환)
        long retUnsigned = Integer.toUnsignedLong(value);
        long threshold = Integer.toUnsignedLong(0xC0000000);

        return retUnsigned >= threshold;
    }

}
public class Main{
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        Advantech_Module module = new Advantech_Module(queue);
        module.start();
    }
}
