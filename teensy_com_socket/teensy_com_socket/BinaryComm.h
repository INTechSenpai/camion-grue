#ifndef _BINARY_COMM_h
#define _BINARY_COMM_h


/**
 * Returns the float written in the vector in [indexStart, indexStart + 3]
 */
float readFloat(std::vector<uint8_t> & io, uint8_t indexStart)
{
  // Little endian processor
    uint8_t b[] = {io.at(indexStart+3), io.at(indexStart + 2), io.at(indexStart + 1), io.at(indexStart)};
    float f;
    memcpy(&f, &b, sizeof(4));
    return f;
}

/**
 * Add the float f at the end of the vector (it uses 4 bytes)
 */
void putFloat(std::vector<uint8_t> & io, float f)
{
  byte *b = (byte *) &f;
  io.push_back(b[3]);
  io.push_back(b[2]);
  io.push_back(b[1]);
  io.push_back(b[0]);
}

#endif
