#include "dxl_pro.h"
#include "XL320.h"


XL320::XL320(HardwareSerial & stream) :
    stream(stream)
{
}

void XL320::begin(uint32_t baudrate, uint32_t timeout)
{
    stream.begin(baudrate);
    stream.setTimeout(timeout);
    setHalfDuplex(stream);
    setWriteMode(stream);
}

void XL320::setPosition(uint8_t id, uint16_t value)
{
	write(id, XL_GOAL_POSITION_L, value, 2);
}

void XL320::setSpeed(uint8_t id, uint16_t value)
{
    write(id, XL_GOAL_SPEED_L, value, 2);
}

void XL320::setLED(uint8_t id, char led_color)
{
	uint8_t val = 0;
    switch (led_color)
    {
    case 'r':
        val = 1;
        break;
    case 'g':
        val = 2;
        break;
    case 'y':
        val = 3;
        break;
    case 'b':
        val = 4;
        break;
    case 'p':
        val = 5;
        break;
    case 'c':
        val = 6;
        break;
    case 'w':
        val = 7;
        break;
    case 'o':
        val = 0;
        break;
    default:
        break;
    }

	write(id, XL_LED, val, 1);
}

void XL320::setTorque(uint8_t id, uint16_t value)
{
    write(id, XL_GOAL_TORQUE, value, 2);
}

void XL320::torqueOn(uint8_t id)
{
	write(id, XL_TORQUE_ENABLE, 1, 1);
}

void XL320::torqueOff(uint8_t id)
{
	write(id, XL_TORQUE_ENABLE, 0, 1);
}

uint16_t XL320::getPresentPosition(uint8_t id)
{
    return read(id, XL_PRESENT_POSITION, 2);
}

uint16_t XL320::getPresentSpeed(uint8_t id)
{
    return read(id, XL_PRESENT_SPEED, 2);
}

uint16_t XL320::getPresentTorque(uint8_t id)
{
    return read(id, XL_PRESENT_LOAD, 2);
}

size_t XL320::write(uint8_t id, uint16_t address, uint16_t value, uint8_t size)
{
    std::vector<uint8_t> params;
    params.push_back(DXL_LOBYTE(address));
    params.push_back(DXL_HIBYTE(address));
    if (size == 1)
    {
        params.push_back((uint8_t)value);
    }
    else if (size == 2)
    {
        params.push_back(DXL_LOBYTE(value));
        params.push_back(DXL_HIBYTE(value));
    }
    else
    {
        return 0;
    }

    Packet packet(id, 0x03, params);
    return packet.writeOnStream(stream);
}

uint16_t XL320::read(uint8_t id, uint16_t address, uint16_t size)
{
    uint16_t ret = UINT16_MAX;
    if (size == 1 || size == 2)
    {
        if (requestPacket(id, address, size) > 0)
        {
            Packet packet = readPacket();
            if (packet.isValid() && packet.getParameterCount() == size + 1)
            {
                uint8_t lw_ret = packet.getParameter(1);
                uint8_t hw_ret = 0;
                if (size == 2)
                {
                    hw_ret = packet.getParameter(2);
                }
                ret = DXL_MAKEWORD(lw_ret, hw_ret);
            }
        }
    }
    return ret;
}

size_t XL320::requestPacket(uint8_t id, uint16_t address, uint16_t size)
{
    std::vector<uint8_t> params;
    params.push_back(DXL_LOBYTE(address));
    params.push_back(DXL_HIBYTE(address));
    params.push_back(DXL_LOBYTE(size));
    params.push_back(DXL_HIBYTE(size));
    Packet packet(id, 0x02, params);
    return packet.writeOnStream(stream);
}

XL320::Packet XL320::readPacket()
{
    Packet packet;
    uint8_t rxBuf[1] = { 0 };
    setReadMode(stream);
    while (packet.isReading())
    {
        if (stream.readBytes(rxBuf, 1) == 1)
        {
            packet.addByte(rxBuf[0]);
        }
        else
        {
            break;
        }
    }
    setWriteMode(stream);
    return packet;
}

XL320::Packet::Packet()
{
    reset();
}

XL320::Packet::Packet(uint8_t id, uint8_t instruction, const std::vector<uint8_t>& parameters)
{
    /* Building an instruction packet */
    data.push_back(0xFF);
    data.push_back(0xFF);
    data.push_back(0xFD);
    data.push_back(0x00);
    data.push_back(id);
    uint16_t length = parameters.size() + 3;
    data.push_back(DXL_LOBYTE(length));
    data.push_back(DXL_HIBYTE(length));
    data.push_back(instruction);
    data.insert(data.end(), parameters.begin(), parameters.end());
    uint16_t checksum = computeChecksum();
    data.push_back(DXL_LOBYTE(checksum));
    data.push_back(DXL_HIBYTE(checksum));
    valid = true;
    reading = false;
}

void XL320::Packet::reset()
{
    data.clear();
    valid = false;
    reading = true;
}

uint8_t XL320::Packet::getId() const
{
    if (data.size() > 4 && valid)
    {
        return data.at(4);
    }
    else
    {
        return -1;
    }
}

uint16_t XL320::Packet::getParameterCount() const
{
    if (data.size() >= 10 && valid)
    {
        return data.size() - 10;
    }
    else
    {
        return 0;
    }
}

uint8_t XL320::Packet::getInstruction() const
{
    if (data.size() > 7 && valid)
    {
        return data.at(7);
    }
    else
    {
        return -1;
    }
}

uint8_t XL320::Packet::getParameter(uint16_t n) const
{
    if ((data.size() > (size_t)(8 + n)) && valid)
    {
        return data.at(8 + n);
    }
    else
    {
        return 0;
    }
}

bool XL320::Packet::isValid() const
{
    return valid;
}

size_t XL320::Packet::printTo(Print & p) const
{
    size_t ret = 0;
    ret += p.print("id=");
    ret += p.println(getId());
    ret += p.print("instruction=");
    ret += p.println(getInstruction());
    uint16_t paramCount = getParameterCount();
    ret += p.print(paramCount);
    ret += p.print(" parameters");
    if (paramCount > 0)
    {
        ret += p.println(":");
    }
    else
    {
        ret += p.println();
    }
    for (uint16_t i = 0; i < paramCount; i++)
    {
        ret += p.println(getParameter(i));
    }
    if (valid)
    {
        ret += p.println("Valid=TRUE");
    }
    else
    {
        ret += p.println("Valid=FALSE");
    }
    return ret;
}

size_t XL320::Packet::writeOnStream(HardwareSerial & stream) const
{
    if (valid)
    {
        uint8_t *data_ptr = (uint8_t*)data.data();
        for (size_t i = 0; i < data.size(); i++)
        {
            Serial.print(data_ptr[i], HEX);
            Serial.print(" ");
        }
        Serial.println();

        size_t ret = stream.write((uint8_t*)data.data(), data.size());
        stream.flush();
        stream.clear();
        return ret;
    }
    else
    {
        return 0;
    }
}

void XL320::Packet::addByte(uint8_t b)
{
    static const uint8_t expectedHeader[3] = { 0xFF, 0xFF, 0xFD };
    if (reading && !valid)
    {
        data.push_back(b);

        size_t dataSize = data.size();
        if (dataSize == 1)
        {
            reading = data.at(0) == expectedHeader[0];
        }
        else if (dataSize == 2)
        {
            reading = data.at(1) == expectedHeader[1];
        }
        else if (dataSize == 3)
        {
            reading = data.at(2) == expectedHeader[2];
        }
        else if (dataSize >= 7)
        {
            uint16_t packetLength = DXL_MAKEWORD(data.at(5), data.at(6));
            if (packetLength < 3)
            {
                reading = false;
            }
            else if (dataSize == (size_t)(packetLength + 7))
            {
                uint16_t checksum = DXL_MAKEWORD(data.at(dataSize - 2), data.at(dataSize - 1));
                valid = checksum == computeChecksum();
                reading = false;
            }
        }
    }
}

bool XL320::Packet::isReading() const
{
    return reading;
}

uint16_t XL320::Packet::computeChecksum() const
{
    if (data.size() > 2)
    {
        return update_crc(0, (unsigned char*)data.data(), data.size() - 2);
    }
    else
    {
        return 0;
    }
}
