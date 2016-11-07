#include <Servo.h> 

// Creates servos for the x and y directions
Servo xServo;
Servo yServo;

// Creates pen servo
Servo penServo;

// Current coordinates of the markerbot
int x = 0;
int y = 0;
int xPrime = 0;
int yPrime = 0;
int neutralPos = 96;
int downPos = 116;

boolean firsttime = true;
float ipr;
int maxSpeedDoubled = 5; // inches/second lateral speed doubled.

void setup() {
  // FIX ATTACHMENTS AND STUFF
  xServo.attach(5); // pin 5 is for the x servo
  yServo.attach(6); // pin 6 is for the y servo
  penServo.attach(3); // pin 3 is for the pen servo
  penServo.write(15);
  penServo.write(neutralPos); 
  Serial.begin(9600);
}

void loop() {

  // Establishes that the Arduino is ready for serial communication
  if (firsttime) {
    firsttime = false;
    Serial.print(">r\r");
    penServo.write(neutralPos); 
  }
  
  // Code
  if (Serial.available() > 0) {
    
    int xCoord = 0;
    int yCoord = 0;
    int xDist;
    int yDist;
    double xInstr;
    double yInstr;
    double pInstr;
    int axd;
    int ayd;
    String temp;
    String pString;
    
    byte incomingByte = Serial.read();

    // Cases for the incoming bytes
    switch (incomingByte) {

      case 'k':
        penServo.write(15);
        break;

      case 'j':
        penServo.write(neutralPos);
        break;
      
      case 'y':
        penServo.write(neutralPos); 
        break;
      
      // These 4 cases are only for the initial marker calibration
      case 'w':
        yServo.write(180);
        delay(100);
        yServo.write(90);
        break;

      case 's':
        yServo.write(0);
        delay(100);
        yServo.write(90);
        break;

      case 'a':
        xServo.write(0);
        delay(100);
        xServo.write(90);
        break;

      case 'd':
        xServo.write(180);
        delay(100);
        xServo.write(90);
        break;
      
      case 'p': // p means it's receiving a new point to move to

        xCoord = 0;
        yCoord = 0;

        while (true) { // xCoord block
          
          incomingByte = Serial.read();
          if (incomingByte == '.') // period moves to yCoord
            break;
          if (incomingByte == 255) // if serial comm is too slow it sends nothing (255)
            continue;
            
          xCoord *= 10;
          xCoord = ((incomingByte - 48) + xCoord);
        }
        
        while (true) { // yCoord block
          
          incomingByte = Serial.read();
          if (incomingByte == '.') // newline ends the reception of data
            break;
          if (incomingByte == 255) // if serial comm is too slow it sends nothing (255)
            continue;
            
          yCoord *= 10;
          yCoord = ((incomingByte - 48) + yCoord);
        }

        while (true) { // boolean block
          
          incomingByte = Serial.read();
          if (incomingByte == '\n') // new line ends this block
            break;
          if (incomingByte == 255) // if serial comm is too slow it sends nothing (255)
            continue;

          // CHANGE THE PINSTR VALUES LATER
          if (incomingByte == 'u') {
            pInstr = neutralPos;
            pString = "PEN UP";
          } else if (incomingByte == 'd') {
            pInstr = downPos;
            pString = "PEN DOWN";
          }
        
        }
        
        xDist = xCoord - x + xPrime;
        yDist = yCoord - y + yPrime;
        axd = abs(xDist);
        ayd = abs(yDist);

        if (axd > ayd) { // x is max speed, y is fraction of max
          
          long time = (long)((2000 * ipr * axd) / maxSpeedDoubled) + 1;
          
          if (xDist < 0) { // pen moves up, send high signal
            xInstr = 180;
            
          } else { // pen moves down, send low signal
            xInstr = 0;
            
          }

          if (yDist < 0) { // pen moves left, send low signal
            yInstr = 90 * (1 - ((double)ayd / (double)axd));
            
          } else { // pen moves right, send high signal
            yInstr = 90 * (1 + ((double)ayd / (double)axd));
            
          }

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(pInstr);
          delay(time);
          xServo.write(90); // So the servo doesn't overshoot
          yServo.write(90);
          Serial.print(">r\r");
          
          x = xCoord + xPrime;
          y = yCoord + yPrime;
          
        } else {
          
          long time = (long)((2000 * ipr * ayd) / maxSpeedDoubled) + 1;
          
          if (xDist < 0) { // pen moves up, send high signal
            xInstr = 90 * (1 + ((double)axd / (double)ayd));
            
          } else { // pen moves down, send low signal
            xInstr = 90 * (1 - ((double)axd / (double)ayd));
            
          }

          if (yDist < 0) { // pen moves left, send low signal
            yInstr = 0;
            
          } else { // pen moves right, send high signal
            yInstr = 180;
            
          }

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(pInstr);
          delay(time);
          xServo.write(90); // So the servo doesn't overshoot
          yServo.write(90);
          Serial.print(">r\r");
          
          x = xCoord + xPrime;
          y = yCoord + yPrime;
          
        }
        
        // Debugging Block
        Serial.print(">X: ");
        Serial.print(x);
        Serial.print(" | Y: ");
        Serial.print(y);
        Serial.print(" | ");
        Serial.print(pString);
        Serial.println("\r");
        break;
        
      case 'c': // c means it's the initial configuration block

        while (true) { // xPrime block
          incomingByte = Serial.read();
          if (incomingByte == '.') // period moves to yCoord
            break;
          if (incomingByte == 255) // if serial comm is too slow it sends nothing (255)
            continue;
          xPrime *= 10;
          xPrime = ((incomingByte - 48) + xPrime);
        }
        
        while (true) { // yPrime block
          incomingByte = Serial.read();
          if (incomingByte == '\n') // newline ends the reception of data
            break;
          if (incomingByte == 255 || incomingByte == 'u') // if serial comm is too slow it sends nothing (255)
            continue;
          yPrime *= 10;
          yPrime = ((incomingByte - 48) + yPrime);
        }

        if (xPrime > yPrime) {
          long time = (long)((2000 * ipr * xPrime) / maxSpeedDoubled) + 1;
          
          xInstr = 0;
          yInstr = 90 * (1 + ((double)yPrime / (double)xPrime));

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(neutralPos); 
          delay(time);
          xServo.write(90);
          yServo.write(90);
          x = xPrime;
          y = yPrime;
          
        } else {
          long time = (long)((2000 * ipr * yPrime) / maxSpeedDoubled) + 1;
          
          xInstr = 90 * (1 - ((double)xPrime / (double)yPrime));
          yInstr = 180;

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(neutralPos); 
          delay(time);
          xServo.write(90);
          yServo.write(90);
          x = xPrime;
          y = yPrime;
          
        }
        
        Serial.print(">r\r");
        // Debugging
        Serial.print(">");
        Serial.print("x': ");
        Serial.print(xPrime);
        Serial.print(", y': ");
        Serial.print(yPrime);
        Serial.print(" | PEN UP");
        Serial.println("\r");
        break;

       case 'q': // q means you're setting the inch-pixel ratio
       
        temp = "";
        
        while (true) {
          
          incomingByte = Serial.read();
          
          if (incomingByte == '\n') // newline ends the reception of data
            break;
          if (incomingByte == 255) // if serial comm is too slow it sends nothing (255)
            continue;
          temp += (char)incomingByte;
        }
        
        ipr = temp.toFloat();

        Serial.print(">r\r");
        Serial.print(">");
        Serial.print("Inch-Pixel Ratio: ");
        Serial.print(ipr);
        Serial.println("\r");
        break;

       case 'z': // z means serial communication has been completed

        // Moves the pen back to the absolute origin.
        if (xPrime > yPrime) {
          long time = (long)((2000 * ipr * xPrime) / maxSpeedDoubled) + 1;
          xInstr = 180;
          yInstr = 90 * (1 - ((double)yPrime / (double)xPrime));

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(neutralPos); 
          delay(time);
          xServo.write(90);
          yServo.write(90);
          
        } else {
          long time = (long)((2000 * ipr * yPrime) / maxSpeedDoubled) + 1;
          xInstr = 90 * (1 + ((double)xPrime / (double)yPrime));
          yInstr = 0;

          xServo.write(xInstr);
          yServo.write(yInstr);
          penServo.write(neutralPos); 
          delay(time);
          xServo.write(90);
          yServo.write(90);
          
        }
        
        Serial.print(">r\r");
        Serial.print(">");
        Serial.print("d");
        Serial.println("\r");
        break;
        
    }
      
  }
  
}

