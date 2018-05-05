# -*- coding: utf-8 -*-

import cwiid
import time
import sys, os
import traceback
import subprocess
import re

UPDATE_PERIOD = 0.05 # seconds

# set bluetooth timeout in ms
BLUETOOTH_TIMEOUT = 500

def connect():
    wm = cwiid.Wiimote()
    # get all connected bluetooth devices
    connected_devices = subprocess.check_output(("hcitool","con"))
    # extract bluetooth MAC addresses
    addresses = re.findall(r"(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))",connected_devices)
    for i in addresses:
        name = subprocess.check_output(("hcitool","name",i[0]))
        # identify Wiimotes. You may need to change this if looking for balance boards
        # we are also assuming that you only have one Wiimote attached at one time,
        # or that we want to set them all to have the same timeout
        if name.strip()=="Nintendo RVL-CNT-01":
            subprocess.call(("hcitool","lst",i[0],str(BLUETOOTH_TIMEOUT*16/10)))
    return wm

def increase_speed():
    global absolute_speed
    i = 0
    while i < len(speed_list) - 1 and absolute_speed > speed_list[i]:
        i += 1
    absolute_speed = speed_list[i]

def decrease_speed():
    global absolute_speed
    if absolute_speed <= speed_list[0]:
        absolute_speed = 0
    else:
        absolute_speed -= 60
        if absolute_speed < 0:
            absolute_speed = 0

connectivity_OK = True

def check_connectivity():
    global connectivity_OK
    last_check_time = 0
    while True:
        if time.time() - last_check_time > 0.3:
            try:
                wii.request_status()
            except ValueError:
                pass
            except RuntimeError:
                print "Wiimote disconnected"
                connectivity_OK = False
                break
            last_check_time = time.time()

while True:
    print "Launching Java server"
    DEVNULL = open(os.devnull, 'wb')
    javaServer = subprocess.Popen(["./../pc/run_easy.sh", "RemoteControl"], cwd="../pc/", stdout=DEVNULL)
    print "Java server launched"

    # Connect to the Wii Remote. If it times out then quit.
    wii = None
    while wii is None:
        print 'Press 1 + 2 on your Wii Remote now ...'
        time.sleep(1)
        try:
            wii = connect()
        except RuntimeError:
            print "Error opening wiimote connection"

    print 'Wii Remote connected...'
    print 'Press PLUS and MINUS together to disconnect and quit.'


    # List of mode : 'wiimote_std' 'wiimote_horizontal' 'nunchuk'
    control_mode = 'wiimote_std'
    wii.led = cwiid.LED1_ON
    absolute_speed = 0
    speed_list = [300, 500, 750, 1000]

    net_open = False
    left_ejector_armed = True
    right_ejector_armed = True

    b_up_p = False
    b_down_p = False
    b_left_p = False
    b_right_p = False
    b_one_p = False
    b_two_p = False
    b_home_p = False
    b_A_p = False
    b_B_p = False

    last_update_time = 0.0

    wii.rpt_mode = cwiid.RPT_BTN | cwiid.RPT_ACC

    connected = False
    while not connected:
        print "Connecting to the java server.."
        try:
            init()
            connected = True
            print "Connected."
        except:
            print "Connection failed. Retrying in 3s."
            traceback.print_tb(sys.exc_info()[2])
            time.sleep(3)

    # thread.start_new_thread(check_connectivity, ())

    try:
        while True:
            buttons = wii.state['buttons']

            if not connectivity_OK:
                break

            # If Plus and Minus buttons pressed
            # together then rumble and quit.
            if buttons - cwiid.BTN_PLUS - cwiid.BTN_MINUS == 0:
                robot_stop()
                print '\nClosing connection ...'
                wii.rumble = 1
                time.sleep(1)
                wii.rumble = 0
                break

            if wii.state['ext_type'] == cwiid.EXT_NUNCHUK:
                if control_mode != 'nunchuk':
                    wii.rpt_mode = cwiid.RPT_BTN | cwiid.RPT_NUNCHUK
                    control_mode = 'nunchuk'
                    wii.led = cwiid.LED3_ON
                    robot_stop()
                    print "Control mode : nunchuk"
            else:
                if control_mode == 'nunchuk':
                    wii.rpt_mode = cwiid.RPT_BTN | cwiid.RPT_ACC
                    control_mode = 'wiimote_std'
                    wii.led = cwiid.LED1_ON
                    robot_stop()
                    print "Control mode : wiimote_std"

            if buttons & cwiid.BTN_HOME:
                if not b_home_p:
                    b_home_p = True
                    if control_mode == 'wiimote_std':
                        control_mode = 'wiimote_horizontal'
                        wii.led = cwiid.LED2_ON
                        print "Control mode : wiimote_horizontal"
                    elif control_mode == 'wiimote_horizontal':
                        control_mode = 'wiimote_std'
                        wii.led = cwiid.LED1_ON
                        print "Control mode : wiimote_std"
                    else:
                        print "Control mode : nunchuk"
            else:
                b_home_p = False

            if control_mode == 'wiimote_horizontal':
                if buttons & cwiid.BTN_RIGHT:
                    if not b_right_p:
                        b_right_p = True
                        pull_up_net()
                else:
                    b_right_p = False

                if buttons & cwiid.BTN_LEFT:
                    if not b_left_p:
                        b_left_p = True
                        pull_down_net()
                else:
                    b_left_p = False

                if buttons & cwiid.BTN_UP:
                    if not b_up_p:
                        b_up_p = True
                        if left_ejector_armed:
                            eject_left_side()
                            left_ejector_armed = False
                        else:
                            rearm_left_side()
                            left_ejector_armed = True
                else:
                    b_up_p = False

                if buttons & cwiid.BTN_DOWN:
                    if not b_down_p:
                        b_down_p = True
                        if right_ejector_armed:
                            eject_right_side()
                            right_ejector_armed = False
                        else:
                            rearm_right_side()
                            right_ejector_armed = True
                else:
                    b_down_p = False

                if buttons & cwiid.BTN_A:
                    if not b_A_p:
                        b_A_p = True
                        if net_open:
                            close_net()
                            net_open = False
                        else:
                            open_net()
                            net_open = True
                else:
                    b_A_p = False

                if not buttons & cwiid.BTN_B:
                    b_B_p = False

                if time.time() - last_update_time > UPDATE_PERIOD:
                    last_update_time = time.time()

                    speed = 0
                    if buttons & cwiid.BTN_2:
                        if buttons & cwiid.BTN_B and not b_B_p:
                            b_B_p = True
                            absolute_speed += 1
                        increase_speed()
                    else:
                        if buttons & cwiid.BTN_B and absolute_speed != 0:
                            b_B_p = True
                            absolute_speed = 0
                        elif not buttons & cwiid.BTN_B:
                            decrease_speed()
                        elif buttons & cwiid.BTN_B and not b_B_p:
                            speed = -300
                    if speed == 0:
                        set_speed(absolute_speed)
                    else:
                        set_speed(speed)

                    acc = wii.state['acc']
                    direction = acc[1] - 126
                    if abs(direction) < 4:
                        direction = 0
                    elif direction > 23:
                        direction = 23
                    elif direction < -23:
                        direction = -23
                    if direction > 0:
                        direction -= 3
                    elif direction < 0:
                        direction += 3
                    set_direction(direction)

            else:
                if buttons & cwiid.BTN_RIGHT:
                    if not b_right_p:
                        b_right_p = True
                        if right_ejector_armed:
                            eject_right_side()
                            right_ejector_armed = False
                        else:
                            rearm_right_side()
                            right_ejector_armed = True
                else:
                    b_right_p = False

                if buttons & cwiid.BTN_LEFT:
                    if not b_left_p:
                        b_left_p = True
                        if left_ejector_armed:
                            eject_left_side()
                            left_ejector_armed = False
                        else:
                            rearm_left_side()
                            left_ejector_armed = True
                else:
                    b_left_p = False

                if buttons & cwiid.BTN_UP:
                    if not b_up_p:
                        b_up_p = True
                        pull_up_net()
                else:
                    b_up_p = False

                if buttons & cwiid.BTN_DOWN:
                    if not b_down_p:
                        b_down_p = True
                        pull_down_net()
                else:
                    b_down_p = False

                if buttons & cwiid.BTN_1:
                    if not b_one_p:
                        b_one_p = True
                        if net_open:
                            close_net()
                            net_open = False
                        else:
                            open_net()
                            net_open = True
                else:
                    b_one_p = False

                if not buttons & cwiid.BTN_B:
                    b_B_p = False

                if time.time() - last_update_time > UPDATE_PERIOD:
                    last_update_time = time.time()

                    speed = 0
                    if buttons & cwiid.BTN_A:
                        if buttons & cwiid.BTN_B and not b_B_p:
                            b_B_p = True
                            absolute_speed += 1
                        increase_speed()
                    else:
                        if buttons & cwiid.BTN_B and absolute_speed != 0:
                            b_B_p = True
                            absolute_speed = 0
                        elif not buttons & cwiid.BTN_B:
                            decrease_speed()
                        elif buttons & cwiid.BTN_B and not b_B_p:
                            speed = -300
                    if speed == 0:
                        set_speed(absolute_speed)
                    else:
                        set_speed(speed)

                    if control_mode == 'nunchuk':
                        stick = wii.state['nunchuk']['stick']
                        direction = 131 - stick[0]
                        direction /= 4
                        if abs(direction) < 4:
                            direction = 0
                        elif direction > 23:
                            direction = 23
                        elif direction < -23:
                            direction = -23
                        if direction > 0:
                            direction -= 3
                        elif direction < 0:
                            direction += 3
                        set_direction(direction)
                    else:
                        acc = wii.state['acc']
                        direction = 128 - acc[0]
                        if abs(direction) < 4:
                            direction = 0
                        elif direction > 23:
                            direction = 23
                        elif direction < -23:
                            direction = -23
                        if direction > 0:
                            direction -= 3
                        elif direction < 0:
                            direction += 3
                        set_direction(direction)
        close()
        print "Connection closed by user"
    except:
        print "Connection closed by server"
        traceback.print_tb(sys.exc_info()[2])
        close()
    javaServer.kill()
    javaServer.wait()

    print "Terminated. Restarting in 3s"
    time.sleep(3)
