package com.hiepnn.mini_messeger.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {
    // Số ms trôi qua từ mốc này sẽ được ghi vào ID thay vì dùng Unix Epoch gốc
    // (1970) để tiết kiệm bit dữ liệu, giúp thuật toán hoạt động bền vững tối đa
    // khoảng 69 năm.
    private static final long EPOCH = 1690848000000L; // 2023-08-01 00:00:00 UTC
    // Tối đa 5 bits cho mỗi ID máy trạm và ID trung tâm dữ liệu. Cho phép
    // chạy đồng thời tối đa 32 máy chủ trong cùng một cụm dữ liệu và tổng cộng 32
    // trung tâm dữ liệu khác nhau (2^5 = 32)
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    // Tối đa 12 bits cho số thứ tự tự tăng trong cùng 1 ms. Cho phép mỗi
    // node tạo tối đa 4096 ID trong mỗi ms (2^12 = 4096)
    private static final long SEQUENCE_BITS = 12L;

    // Khi ghép các thành phần thành ID 64-bit, Worker ID cần dịch trái 12 bit
    // (bằng số lượng bit của phần Sequence nằm phía cuối)
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID = 1;
    private static final long DATACENTER_ID = 1;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
