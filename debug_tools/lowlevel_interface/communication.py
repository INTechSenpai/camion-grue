
class Communication:
    def __init__(self):
        pass



class Message:
    def __init__(self, ID, data=None, standard=True):
        self.id = ID
        if data is None:
            self.data = []
        else:
            self.data = data
        self.standard = standard
