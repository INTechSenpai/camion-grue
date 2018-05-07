/*
 * dxl_pro.h
 *
 *  Created on: 2013. 4. 25.
 *      Author: ROBOTIS,.LTD.
 */

#ifndef DXL_PRO_H_
#define DXL_PRO_H_

#include <inttypes.h>

#ifdef __cplusplus
extern "C" {
#endif


#define MAXNUM_TXPACKET     (65535)
#define MAXNUM_RXPACKET     (65535)

///////////////// utility for value ///////////////////////////
#define DXL_MAKEWORD(a, b)      ((uint16_t)(((uint8_t)(((uint32_t)(a)) & 0xff)) | ((uint16_t)((uint8_t)(((uint32_t)(b)) & 0xff))) << 8))
#define DXL_MAKEDWORD(a, b)     ((uint32_t)(((uint16_t)(((uint32_t)(a)) & 0xffff)) | ((uint32_t)((uint16_t)(((uint32_t)(b)) & 0xffff))) << 16))
#define DXL_LOWORD(l)           ((uint16_t)(((uint32_t)(l)) & 0xffff))
#define DXL_HIWORD(l)           ((uint16_t)((((uint32_t)(l)) >> 16) & 0xffff))
#define DXL_LOBYTE(w)           ((uint8_t)(((uint32_t)(w)) & 0xff))
#define DXL_HIBYTE(w)           ((uint8_t)((((uint32_t)(w)) >> 8) & 0xff))

uint16_t update_crc(uint16_t crc_accum, const uint8_t *data_blk_ptr, uint16_t data_blk_size);

#ifdef __cplusplus
}
#endif



#endif /* DXL_PRO_H_ */
