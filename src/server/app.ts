import express from 'express';
import cors from 'cors';
import compression from 'compression';
import dotenv from 'dotenv';
import tvProRoutes from './routes/tvpro.routes.js';

dotenv.config();

const app = express();

app.disable('x-powered-by');
app.use(compression());
app.use(cors());
app.use(express.json());

app.use('/api/v1', tvProRoutes);

app.get('/', (_req, res) => {
  res.json({
    name: 'APK-TVpro Express Gateway API',
    version: '1.0.0',
    documentation: '/api/v1/health'
  });
});

export default app;
