# Artemis Messenger 3.0.0

## What is Artemis Messenger?

Artemis Messenger is an Android app designed to be used as a partial Comms client for Artemis Spaceship Bridge Simulator. Its original purpose was to receive and parse incoming Comms messages about side missions and organize the information into a neat, accessible and readable table, making it much easier to keep track of what side missions are available, what rewards they offer, and how much progress has been made. Eventually, it was expanded to include status details reported from ally ships and stations as well.

## What is a side mission?

A side mission is a task within a simulation that entails the transport of supplies or data from one location to another, with a reward offered for doing so. Generally, when the player receives a message about a new side mission, the message begins with the line "Help us help you". It then lists specifics about the task itself, namely the location to visit first and the reward that is offered by the sender of the message; the ship or station that sent the message is the location to visit after the location mentioned in the content of the message. When one of the two locations is visited in order, that location sends a message that is parsed by the app, and the app updates the status of the side mission accordingly. The next location to visit is highlighted in orange. When a mission is completed, it turns green; it can then be removed from the table by touching it.

The app parses data from all messages relevant to side missions and organizes it into a table with three columns: Source, Destination and Reward. The Source is the first location to visit, the Destination is the second, and the Reward is what you receive for completing the mission. There are five types of rewards: Battery Charge (extra energy), Extra Coolant (extra coolant for Engineering), Nuclear Missiles (two Type 4 Nuke torpedoes), Production Speed (doubled at Destination station) and Shield Boost (stronger front and rear shields). The side missions in the table can be filtered by their type.

## What about ally ships and stations?

This app also has the ability to monitor the statuses of ally ships and stations! At the start of a simulation, and at regular intervals throughout, the app will send messages to ally ships requesting a hail from them. The message received in response is then parsed and used to populate the Allies table with information on the ally ships. The information is written in three columns: Ship (the ID and name of the ship), Shields (front and rear shield strength) and Status (all other information). The information reported in Status is as follows: ships can be normal, flying blind, malfunctioning, commandeered, or traps. They may have some side missions for you, and they may also have some spare energy, or in a Deep Strike mission, spare torpedoes. This information is obtained through messages, but the Shields information is not; it is updated live by a system manager.

The stations are managed using a similar system. Instead of interval updates, the stations give status updates when various events occur, such as docking, undocking, and the completion of missile production. There are two columns in this table: Station and Ordnance. The Station column includes the name and type of the station, its shield strength and any side missions or replacement fighters it has. Like ally ships, stations' shield strengths are updated in real time. The Ordnance column includes stock information on a station's Homing, Nuke, Mine, EMP and Plasma shock torpedoes, as well as a listing of what ordnance is currently being built. At any time, a station's row in the table can be touched in order to send a request to stand by for docking.

## What else is planned for this app?

Currently, this app is not compatible with the latest version of Artemis, which is version 2.6.0. Fixing the compatibility issues requires updates to a library used by this app (see Credits for more details) that accommodate the new features added in Artemis 2.6.0. There are two bug fixes also intended for the next version of the app, as well as a new feature whereby the app uses a routing algorithm to calculate the shortest possible route to complete all of the current side missions. 

## Credits

This app uses [IAN](http://github.com/rjwut/ian) (Interface for Artemis Networking), a Java library written by Robert J. Walker. It has been adapted for various purposes, including backwards compatibility with Artemis 2.3.0 and later. The 3D modeling features have been removed as they are not needed and are not compatible with Android.