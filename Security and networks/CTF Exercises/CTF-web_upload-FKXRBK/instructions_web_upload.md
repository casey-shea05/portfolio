# Exercise 4: Web Security
## Introduction
We got an anonymous tip about a website with some possibly shady dealings going on. You've been assigned the task to analyse the website. The website contains many security vulnerabilities which can be used to retrieve the flags for this assignment.

### General guidance
You can find a stripped down version of the source code of the website in the `html` directory in the challenge folder you found this file in. Each challenge zip should contain a copy, but they will all be the same exact files so you can extract just the instructions if you wish.
Some specific source files have been excluded from the folder we give you - if these files are relevant to the task we will say so in the instructions.

Many of the website vulnerabilities can be seen by reading and understanding the source code. Although you have not been taught some of the relevant languages such as PHP, part of this exercise is about learning how websites and web scripting languages work (and how easily they can be exploited).
Don't be afraid to look up documentation or guidance about PHP - theres a lot of it out there.

All your attacks on the website must be carried out via the website (i.e. over port 443). Attacks targeting the server infrastructure such as [Denial of Service attacks](https://en.wikipedia.org/wiki/Denial-of-service_attack) are not in-scope.
Generally, please don't bring down the website server (intentionally), we will know!

# Task: File upload attack
**Get access to the database**: Find a *file upload attack* and use it to upload some php that lets you view the source code of the `sql.php` page. In this file you will find the location of the sql database file. Use this to leak the contents of the database where you will find another flag. Submit this flag to the flag submission website.

The `sql.php` file isn't included in the source we've given you - you will need to figure out its location from the rest of it. The same goes for the database file.

### Website Access
The website is hosted at <https://sensiblefurniture.teachingvms.aws.cs.bham.ac.uk>  
You will need to login in order for the website to give you your unique flag. You can login via the `signin` page - <https://sensiblefurniture.teachingvms.aws.cs.bham.ac.uk/signin.php>
Your login details will be your *student username* (i.e. the one at the start of your University email in the format abc123) and the *token* you use on the flag submission website.
If you have been using one of the 'test' email and tokens, use those as your login details.

### Getting Help
The website [The OWASP Top 10](https://owasp.org/www-project-top-ten/) provides an excellent description of the kinds of web attacks tou can use to complete these exercises.
You can come to the lab sessions with questions and to get extra help with the exercise. You can also use the discussions on the Teams channel for questions or issues.
If you find an issue with the exercise website or the flag website, please email one of the module leads or one of the TAs, you can find their contact details in the Canvas module.

