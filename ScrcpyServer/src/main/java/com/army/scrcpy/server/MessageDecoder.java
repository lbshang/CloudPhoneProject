package com.army.scrcpy.server;

import com.sun.org.apache.bcel.internal.classfile.ConstantValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author army
 * @version V_1.0.0
 * @date 2020-04-04
 * @description
 */
public class MessageDecoder extends ByteToMessageDecoder {

    public static final int BASE_LENGTH = 7;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        // 可读长度必须大于基本长度
        if (buffer.readableBytes() < BASE_LENGTH) {
            return;
        }
        // 记录包头开始的index
        int beginReader;

        while (true) {
            // 获取包头开始的index
            beginReader = buffer.readerIndex();
            // 标记包头开始的index
            buffer.markReaderIndex();
            // 读到了协议的开始标志，结束while循环
            if (buffer.readShort() == SocketConstants.DATA_BEGIN) {
                break;
            }

            // 未读到包头，略过一个字节
            // 每次略过，一个字节，去读取，包头信息的开始标记
            buffer.resetReaderIndex();
            buffer.readByte();

            // 当略过，一个字节之后，
            // 数据包的长度，又变得不满足
            // 此时，应该结束。等待后面的数据到达
            if (buffer.readableBytes() < BASE_LENGTH) {
                return;
            }
        }

        if (buffer.readableBytes() == 0) {
            buffer.readerIndex(beginReader);
            return;
        }
        byte type = buffer.readByte();


        if (type == SocketConstants.CONTROLMSG_TYPE) {
            if (buffer.readableBytes() < 4) {
                buffer.readerIndex(beginReader);
                return;
            }
            int length = buffer.readInt();
            if (buffer.readableBytes() < length) {
                buffer.readerIndex(beginReader);
            } else {
                ByteBuf byteBuf = Unpooled.buffer(1 + length);
                byteBuf.writeByte(type);
                byteBuf.writeBytes(buffer, length);
                out.add(byteBuf);
            }
            return;
        }

        if (type == SocketConstants.VIDEOSTREAM_TYPE) {
            if (buffer.readableBytes() < 8 + 4) {
                buffer.readerIndex(beginReader);
                return;
            }
            long pts = buffer.readLong();
            int length = buffer.readInt();
            if (buffer.readableBytes() < length) {
                buffer.readerIndex(beginReader);
            } else {
                ByteBuf byteBuf = Unpooled.buffer(1 + 8 + length);
                byteBuf.writeByte(type);
                byteBuf.writeLong(pts);
                byteBuf.writeBytes(buffer, length);
                out.add(byteBuf);
            }
            return;
        }

        buffer.readerIndex(beginReader);
    }
}
