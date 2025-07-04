# Use a Node.js image to build the React app
FROM node:18-alpine as builder
WORKDIR /app
COPY package.json ./ # Copy package.json and yarn.lock/package-lock.json first for caching
COPY yarn.lock ./ # if you use yarn
RUN npm install
COPY . ./ # Copy the rest of the application code
RUN npm run build

# Use a lightweight web server to serve the static files
FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
