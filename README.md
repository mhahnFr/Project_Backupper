# Welcome to the Project_Backupper!
This repository contains a project I made in 2017. It is an application that
creates a backup of the registered folders in the specified folder. It requires
a Java runtime environment in version 9 or higher.

## Idea
The initial idea of this project was to build an application, that creates a
backup of the registered files in the specified location. Only the files and
folders that have changed should be copied over, the other files should be
linked to the previous backup. Additionally, the application should be able to
create backups using the local network. Also, multiple profiles, each with a
specified backup location and associated folders to be saved, should be
avalaible.

## Approach
Besides a simple GUI with basic control of the application, an extensive
settings window has been implemented. The actual backup process is quite
straight forward: each registered folder is recursively copied over to the
specified saving location. Each file, whose last edited timestamp has not
changed, is hardlinked from the previous backup. If a file did not exist in the
previous backup or there are no previous backups, such a file is copied over.

### Network
As some virtual machines can see the hosts filesystem, but the hosts cannot see
the guest's filesystem, this application has the possibility to run in an
server/client mode. This application is simply ran in the assistent mode on
such a virtual machine, the folders to be saved have to be registered on the
assistent. The profile on the host is then simply set up with the assistent
informations, such as port number, hostname etc. For each file that has to be
saved, the assistent is asked for the last modified timestamp of the file. If
it has to copied, the assistent is requested to copy the file.

### Final notes
Backups that are older than one day are currently deleted.

This project is in a working state, and might be updated in the future.

© 2017 [mhahnFr](https://www.github.com/mhahnFr)
