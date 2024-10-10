# Threadli

Threadli is a team collaboration app designed to facilitate organized and asynchronous communication. Unlike traditional chat tools like Slack or Microsoft Teams, Threadli focuses on threads rather than real-time messaging, allowing team members to collaborate without the pressure of responding immediately. This makes it ideal for remote teams or those working across different time zones.

Key features of Threadli include:

- Threaded Conversations: Each conversation is organized by topic, making it easier to follow discussions and avoid information overload.
- Asynchronous Communication: Team members can reply at their own pace, promoting a more thoughtful and less distracting work environment.
- Integrations: Threadli integrates with various tools, such as project management and file-sharing apps, to streamline workflows.
- Focus on Clarity: It’s built for deep work, with fewer interruptions compared to traditional chat-based tools.
- Threadli is especially useful for teams that prioritize focus and long-term project collaboration rather than constant back-and-forth communication.


### Project Setup 

First create a database named threadli 

```
CREATE DATABASE threadli CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

Define the following environment variables (E.g. Mac OSX environment file zshrc):

```
export threadli_db_url=jdbc:mysql://127.0.0.1:3306/threadli
export async_db_user=user
export async_db_password=password
export async_email_from=name@team.com
export async_mail_host=mail.google.com
export async_mail_username=name@team.com
export async_mail_password=password
export async_host_url=https://team.com
export async_db_driver=com.mysql.cj.jdbc.Driver
export async_db_dialect=org.hibernate.dialect.MySQL8Dialect
export async_mail_port=587
```

Warning: File uploads do not work when using SQLite.