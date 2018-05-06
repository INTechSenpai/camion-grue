#include "dxl_pro.h"
#include "XL320.h"


XL320::XL320() {}

XL320::~XL320() {}

void XL320::begin(Stream &stream)
{
    this->stream = &stream;
    this->setHalfDuplex(stream);
    this->setWriteMode(stream);
}

void XL320::moveJoint(int id, int value)
{
	writeDouble(id, XL_GOAL_POSITION_L, value);
}

void XL320::setJointSpeed(int id, int value)
{
    writeDouble(id, XL_GOAL_SPEED_L, value);
}

void XL320::LED(int id, char led_color)
{
	int val = 0;
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

	write(id, XL_LED, val);
}

void XL320::setJointTorque(int id, int value)
{
    writeDouble(id, XL_GOAL_TORQUE, value);
}

void XL320::TorqueON(int id)
{
	write(id, XL_TORQUE_ENABLE, 1);
}

void XL320::TorqueOFF(int id)
{
	write(id, XL_TORQUE_ENABLE, 0);
}

int XL320::getJointPosition(int id)
{
    return read(id, XL_PRESENT_POSITION, 2);
}

int XL320::write(int id, int Address, int value)
{
    return write(id, Address, value, 1);
}

int XL320::writeDouble(int id, int Address, int value)
{
    return write(id, Address, value, 2);
}

int XL320::write(int id, int Address, int value, int size)
{

    /*Dynamixel 2.0 communication protocol
      used by Dynamixel XL-320 and Dynamixel PRO only.
    */

    const int bufsize = 16;
    byte txbuffer[bufsize];

    if (size == 1)
    {
        Packet p(txbuffer, bufsize, id, 0x03, 3,
            DXL_LOBYTE(Address),
            DXL_HIBYTE(Address),
            DXL_LOBYTE(value));
        stream->write(txbuffer, p.getSize());
        this->stream->flush();
        return p.getSize();
    }
    else if (size == 2)
    {
        Packet p(txbuffer, bufsize, id, 0x03, 4,
            DXL_LOBYTE(Address),
            DXL_HIBYTE(Address),
            DXL_LOBYTE(value),
            DXL_HIBYTE(value));
        stream->write(txbuffer, p.getSize());
        this->stream->flush();
        return p.getSize();
    }
    else
    {
        return -1;
    }
}



int XL320::read(int id, int Address, int size)
{
    unsigned char buffer[255];

    requestPacket(id, Address, size);
    this->setReadMode(*(this->stream));
    int ret = this->readPacket(buffer, 255);
    this->setWriteMode(*(this->stream));

    if (ret > 0) {
        Packet p(buffer, 255);
        if (p.isValid() && p.getParameterCount() >= size + 1) {
            int ret = 0;
            for (int i = 0; i < size; i++) {
                ret += (p.getParameter(i + 1) << (8*i));
            }
            return ret;
        }
        else {
            return -1;
        }
    }
    return -2;
}

int XL320::requestPacket(int id, int Address, int size){

	/*Dynamixel 2.0 communication protocol
	  used by Dynamixel XL-320 and Dynamixel PRO only.
	*/

    const int bufsize = 16;

    byte txbuffer[bufsize];

    Packet p(txbuffer,bufsize,id,0x02,4,
	DXL_LOBYTE(Address),
	DXL_HIBYTE(Address),
	DXL_LOBYTE(size),
	DXL_HIBYTE(size));

    stream->write(txbuffer,p.getSize());
    this->stream->flush();

    return p.getSize();	
}

// from http://stackoverflow.com/a/133363/195061

#define FSM
#define STATE(x)        s_##x : if(!stream->readBytes(&BUFFER[I++],1)) goto sx_timeout ; if(I>=SIZE) goto sx_overflow; sn_##x :
#define THISBYTE        (BUFFER[I-1])
#define NEXTSTATE(x)    goto s_##x
#define NEXTSTATE_NR(x) goto sn_##x
#define OVERFLOW        sx_overflow :
#define TIMEOUT         sx_timeout :

int XL320::readPacket(unsigned char *BUFFER, size_t SIZE)
{
    int C;
    int I = 0;    

    int length = 0;

      // state names normally name the last parsed symbol
      

    FSM {
      STATE(start) {
	if(THISBYTE==0xFF) NEXTSTATE(header_ff_1);
	I=0; NEXTSTATE(start);
      }
      STATE(header_ff_1) {
	if(THISBYTE==0xFF) NEXTSTATE(header_ff_2);
	I=0; NEXTSTATE(start);	
      }
      STATE(header_ff_2) {
	if(THISBYTE==0xFD) NEXTSTATE(header_fd);
	// yet more 0xFF's? stay in this state
	if(THISBYTE==0xFF) NEXTSTATE(header_ff_2);
	// anything else? restart
	I=0; NEXTSTATE(start);
      }
      STATE(header_fd) {
	  // reading reserved, could be anything in theory, normally 0
      }
      STATE(header_reserved) {
	  // id = THISBYTE
      }
      STATE(id) {
	length = THISBYTE;
      }
      STATE(length_1) {
	length += THISBYTE<<8; // eg: length=4
      }
      STATE(length_2) {
      }
      STATE(instr) {
	// instr = THISBYTE
        // check length because
        // action and reboot commands have no parameters
	if(I-length>=5) NEXTSTATE(checksum_1);
      }
      STATE(params) {
	  // check length and maybe skip to checksum
	  if(I-length>=5) NEXTSTATE(checksum_1);
	  // or keep reading params
	  NEXTSTATE(params);
      }
      STATE(checksum_1) {
      }
      STATE(checksum_2) {
	  // done
	  return I; 
      }
      OVERFLOW {
          return -1;
      }
      TIMEOUT {
	  return -2;
      }

    }
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
    uint8_t lw_length = (uint8_t)(length & 0xFF);
    uint8_t hw_length = (uint8_t)((length >> 8) & 0xFF);
    data.push_back(lw_length);
    data.push_back(hw_length);
    data.push_back(instruction);
    data.insert(data.end(), parameters.begin(), parameters.end());
    uint16_t checksum = computeChecksum();
    uint8_t lw_checksum = (uint8_t)(checksum & 0xFF);
    uint8_t hw_checksum = (uint8_t)((checksum >> 8) & 0xFF);
    data.push_back(lw_checksum);
    data.push_back(hw_checksum);
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
    if (data.size() > 8 + n && valid)
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

size_t XL320::Packet::writeOnStream(Stream & stream) const
{
    if (valid)
    {
        return stream.write((uint8_t*)data.data(), data.size());
    }
    else
    {
        return 0;
    }
}

void XL320::Packet::addByte(uint8_t b)
{
    if (reading)
    {
        data.push_back(b);

        if (data.size() == 3)
        {

        }
        else if ()
        {

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
