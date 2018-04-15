#ifndef _SINGLETON_h
#define _SINGLETON_h

template<class T>
class Singleton
{
public:
    static T& Instance()
    {
        static T instance;
        return instance;
    }
protected:
    Singleton() {}
};


#endif
