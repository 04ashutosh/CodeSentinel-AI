import os
import sys

class DataPipeline:
    def __init__(self, name):
        self.name = name

    def execute_batch(self, batch_size):
        return batch_size * 2