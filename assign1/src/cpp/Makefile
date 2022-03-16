CC = g++
CPPFLAGS = -O2
CPPLIBS = -lpapi
PROG_NAME = matrixmul

SRCDIR = src

CPP_SOURCES := $(shell find $(SRCDIR) -name '*.cpp')
CPP_OBJECTS := $(CPP_SOURCES:%.cpp=%.o)

.PHONY: clean

all: $(PROG_NAME)-cpp

$(PROG_NAME)-cpp: $(CPP_OBJECTS)
	$(CC) $(CPPFLAGS) $(CPPLIBS) -o $(PROG_NAME)-cpp $(CPP_OBJECTS)

clean:
	rm -f $(PROG_NAME)-cpp $(CPP_OBJECTS)
