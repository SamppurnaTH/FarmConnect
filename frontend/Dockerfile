# Stage 1: Build the React application
FROM node:20 AS builder

WORKDIR /app

# Copy package descriptors and cleanly install strict dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the application
COPY . .

# Execute Vite Build to generate optimized static files in /dist
RUN npm run build

# Stage 2: Serve the application using NGINX Alpine
FROM nginx:alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy built static files from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Apply SPA routing config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
