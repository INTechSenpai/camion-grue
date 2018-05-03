/*
 * dxl_pro.h
 *
 *  Created on: 2013. 4. 25.
 *      Author: ROBOTIS,.LTD.
 */

#ifndef DXL_PRO_H_
#define DXL_PRO_H_

#ifdef __cplusplus
extern "C" {
#endif


#define MAXNUM_TXPACKET     (65535)
#define MAXNUM_RXPACKET     (65535)

///////////////// utility for value ///////////////////////////
#define DXL_MAKEWORD(a, b)      ((unsigned short)(((unsigned char)(((unsigned long)(a)) & 0xff)) | ((unsigned short)((unsigned char)(((unsigned long)(b)) & 0xff))) << 8))
#define DXL_MAKEDWORD(a, b)     ((unsigned int)(((unsigned short)(((unsigned long)(a)) & 0xffff)) | ((unsigned int)((unsigned short)(((unsigned long)(b)) & 0xffff))) << 16))
#define DXL_LOWORD(l)           ((unsigned short)(((unsigned long)(l)) & 0xffff))
#define DXL_HIWORD(l)           ((unsigned short)((((unsigned long)(l)) >> 16) & 0xffff))
#define DXL_LOBYTE(w)           ((unsigned char)(((unsigned long)(w)) & 0xff))
#define DXL_HIBYTE(w)           ((unsigned char)((((unsigned long)(w)) >> 8) & 0xff))

unsigned short update_crc(unsigned short crc_accum, unsigned char *data_blk_ptr, unsigned short data_blk_size);

#ifdef __cplusplus
}
#endif



#endif /* DXL_PRO_H_ */
