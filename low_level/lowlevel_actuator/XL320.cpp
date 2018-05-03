/*
    Crappy code from the Arduino community
    Credits have been removed to preserve the author's reputation
    Reworked in order to be usable and almost readable (and to remove warnings)
 */

#include "Arduino.h"
#include "dxl_pro.h"
#include "XL320.h"
#include <stdlib.h>
#include <stdarg.h>	


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


XL320::Packet::Packet(
	unsigned char *data,
	size_t data_size,
	unsigned char id,
	unsigned char instruction,
	int parameter_data_size,
	...)
{
    // [ff][ff][fd][00][id][len1][len2] { [instr][params(parameter_data_size)][crc1][crc2] }
    unsigned int length=3+parameter_data_size;
    if(!data) {
	// [ff][ff][fd][00][id][len1][len2] { [data(length)] }
	this->data_size = 7+length;   
	this->data = (unsigned char*)malloc(data_size);
	this->freeData = true;
    } else {
	this->data = data;
	this->data_size = data_size;
	this->freeData = false;
    }
    this->data[0]=0xFF;
    this->data[1]=0xFF;
    this->data[2]=0xFD;
    this->data[3]=0x00;
    this->data[4]=id;
    this->data[5]=length&0xff;
    this->data[6]=(length>>8)&0xff;
    this->data[7]=instruction;
    va_list args;
    va_start(args, parameter_data_size); 
    for(int i=0;i<parameter_data_size;i++) {
	unsigned char arg = va_arg(args, int);
	this->data[8+i]=arg;
    }
    unsigned short crc = update_crc(0,this->data,this->getSize()-2);
    this->data[8+parameter_data_size]=crc&0xff;
    this->data[9+parameter_data_size]=(crc>>8)&0xff;
    va_end(args);
}

XL320::Packet::Packet(unsigned char *data, size_t size)
{
    this->data = data;
    this->data_size = size;
    this->freeData = false;
}

XL320::Packet::~Packet()
{
    if(this->freeData==true) {
	    free(this->data);
    }
}

void XL320::Packet::toStream(Stream &stream)
{
    stream.print("id: ");
    stream.println(this->getId(),DEC);
    stream.print("length: ");
    stream.println(this->getLength(),DEC);
    stream.print("instruction: ");
    stream.println(this->getInstruction(),HEX);
    stream.print("parameter count: ");
    stream.println(this->getParameterCount(), DEC);
    for(int i=0;i<this->getParameterCount(); i++) {
	    stream.print(this->getParameter(i),HEX);
	    if(i<this->getParameterCount()-1) {
	        stream.print(",");
	    }
    }
    stream.println();
    stream.print("valid: ");
    stream.println(this->isValid()?"yes":"no");
}

unsigned char XL320::Packet::getId()
{
    return data[4];
}

int XL320::Packet::getLength()
{
    return data[5]+((data[6]&0xff)<<8);
}

int XL320::Packet::getSize()
{
    return getLength()+7;
}

int XL320::Packet::getParameterCount()
{
    return getLength()-3;
}

unsigned char XL320::Packet::getInstruction()
{
    return data[7];
}

unsigned char XL320::Packet::getParameter(int n)
{
    return data[8+n];
}

bool XL320::Packet::isValid()
{
    int length = getLength();
    unsigned short storedChecksum = data[length+5]+(data[length+6]<<8);
    return storedChecksum == update_crc(0,data,length+5);
}
