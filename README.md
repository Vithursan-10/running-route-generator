🗺️ Route Generator
📌 Overview

The Route Generator is a fullstack application that generates back-and-forth running routes of a given distance starting from the user’s location. It integrates OpenStreetMap (OSM) data with a Spring Boot backend and a React.js frontend to create custom routes using my custom made algorithm.

I built this project after realising that current running route generators either require a subscription (Strava) or do not start from my location and require me to travel somewhere to do. This sparked an idea of creating my own running route generator that satisfies my needs.

🚀 Features

Custom Route Generation: Input distance → generate unique running routes

Reset & Regenerate for multiple options

Export Routes as GPX for GPS devices or running tracker apps like Strava

Cache System for up to 6 to avoid recomputation for identical requests

Interactive UI Toolbar for distance input and controls

🛠️ Tech Stack

Frontend: React.js, CSS

Backend: Spring Boot, RESTful APIs, Maven

Data & APIs:

OpenStreetMap (OSM) for mapping data

Geolocation API for starting point detection


📸 Demo

<img width="1847" height="870" alt="image" src="https://github.com/user-attachments/assets/8a62425f-3e88-4fa0-b1e4-91d5fc9c4165" />

<img width="1848" height="858" alt="image" src="https://github.com/user-attachments/assets/80336673-e28f-4621-9c55-896b76b3feae" />

<img width="1827" height="648" alt="image" src="https://github.com/user-attachments/assets/be36b07e-6d8c-42f1-8bff-cc5f1ed1e4bb" />



📂 Setup

FIRST DOWNLOAD OSM DATA FILE FOR WANTED AREA AND PUT IN PROJECT FOLDER    (e.g. for Greater London: https://download.geofabrik.de/europe/united-kingdom/england/greater-london.html)

Clone the repository

git clone https://github.com/your-username/route-generator.git
cd route-generator


Start backend

cd backend
mvn spring-boot:run


Start frontend

cd frontend
npm install
npm start
