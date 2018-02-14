#ifndef _COMMAND_h
#define _COMMAND_h

#include <vector>
#include <Printable.h>

#define COMMAND_MAX_DATA_SIZE    254


class Command : public Printable
{
public:
    Command()
    {
        commandValid = false;
    }

    Command(uint8_t source, std::vector<uint8_t> const & command)
    {
        this->source = source;
    // on vérifie la longueur de la trame
        if (command.size() < 2 || command.size() > COMMAND_MAX_DATA_SIZE + 2)
        {
            commandValid = false;
            return;
        }
        else
        {
            id = command.at(0);
      // vérification de la longueur de la trame
            if (command.at(1) != command.size() - 2)
            {
                commandValid = false;
                return;
            }
            else
            {
                for (size_t i = 2; i < command.size(); i++)
                {
                    data.push_back(command.at(i));
                }
                commandValid = true;
            }
        }
    }

    Command(uint8_t source, uint8_t id, std::vector<uint8_t> const & data)
    {
        this->source = source;
        this->id = id;
        if (data.size() <= COMMAND_MAX_DATA_SIZE)
        {
            this->data = data;
            commandValid = true;
        }
        else
        {
            commandValid = false;
        }
    }

    Command(uint8_t source, uint8_t id)
    {
        this->source = source;
        this->id = id;
        commandValid = true;
    }

    bool isValid() const
    {
        return commandValid;
    }

    void makeInvalid()
    {
        commandValid = false;
    }

    uint8_t getSource() const
    {
        return source;
    }

    uint8_t getId() const
    {
        return id;
    }

    uint8_t getLength() const
    {
        return (uint8_t)data.size();
    }

    std::vector<uint8_t> getData() const
    {
        return data;
    }

    std::vector<uint8_t> getVector() const
    {
        std::vector<uint8_t> output;
        output.push_back(id);
        output.push_back((uint8_t)data.size());
        for (size_t i = 0; i < data.size(); i++)
        {
            output.push_back(data.at(i));
        }
        return output;
    }

    size_t printTo(Print& p) const
    {
        size_t n = 0;
        if (!commandValid)
        {
            n += p.print("[INVALID] ");
        }
        n += p.print("S:");
        n += p.print(source);
        n += p.print(" ID:");
        n += p.print(id, HEX);
        if (data.size() > 0)
        {
            n += p.print(" Data: ");
            for (size_t i = 0; i < data.size(); i++)
            {
                n += p.print(data.at(i), HEX);
                n += p.print(" ");
            }
        }
        return n;
    }

private:
    bool commandValid;
    uint8_t source; // le numéro du client d'où provient cette commande
    uint8_t id; // l'ID de l'ordre de la commande
    std::vector<uint8_t> data;
};


#endif

