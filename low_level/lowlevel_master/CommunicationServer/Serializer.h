#ifndef _SERIALIZER_h
#define _SERIALIZER_h

#include <vector>


class Serializer
{
public:
    Serializer() {}

    static int32_t readInt(const std::vector<uint8_t> &input, size_t &start)
    {
        uint8_t buf[4] = { 
            input.at(start), 
            input.at(start + 1), 
            input.at(start + 2), 
            input.at(start + 3)
        };
        start += 4;
        return *((int32_t*)buf);
    }

    static uint32_t readUInt(const std::vector<uint8_t> &input, size_t &start)
    {
        int32_t val = readInt(input, start);
        if (val < 0)
        {
            val = 0;
        }
        return (uint32_t)val;
    }

    static float readFloat(const std::vector<uint8_t> &input, size_t &start)
    {
        uint8_t buf[4] = {
            input.at(start),
            input.at(start + 1),
            input.at(start + 2),
            input.at(start + 3)
        };
        start += 4;
        return *((float*)buf);
    }

    static bool readBool(const std::vector<uint8_t> &input, size_t &start)
    {
        return (bool)input.at(start++);
    }

    static uint8_t readEnum(const std::vector<uint8_t> &input, size_t &start)
    {
        return (uint8_t)input.at(start++);
    }

    static void writeInt(int32_t value, std::vector<uint8_t> & output)
    {
        uint8_t *buf = (uint8_t *)(&value);
        output.push_back(buf[0]);
        output.push_back(buf[1]);
        output.push_back(buf[2]);
        output.push_back(buf[3]);
    }

    static void writeUInt(uint32_t value, std::vector<uint8_t> & output)
    {
        if (value > INT32_MAX)
        {
            value = INT32_MAX;
        }
        writeInt(value, output);
    }

    static void writeFloat(float value, std::vector<uint8_t> & output)
    {
        uint8_t *buf = (uint8_t *)(&value);
        output.push_back(buf[0]);
        output.push_back(buf[1]);
        output.push_back(buf[2]);
        output.push_back(buf[3]);
    }

    static void writeBool(bool value, std::vector<uint8_t> & output)
    {
        output.push_back((uint8_t)value);
    }

    static void writeEnum(uint8_t value, std::vector<uint8_t> & output)
    {
        output.push_back(value);
    }
};


#endif
