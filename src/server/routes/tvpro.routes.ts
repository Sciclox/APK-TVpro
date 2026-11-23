import { Router } from 'express';
import { cache } from '../cache.js';
import { MovieScraperEngine, SCRAPED_MOVIES } from '../scraper/movieScraper.js';

const router = Router();
const CACHE_TTL_LIST = 15 * 60 * 1000; // 15 minutos
const CACHE_TTL_EXTRACT = 30 * 60 * 1000; // 30 minutos

// GET /api/v1/list?page=1
router.get('/list', async (req, res) => {
  const page = parseInt(String(req.query.page || '1'), 10) || 1;
  const cacheKey = `list_page_${page}`;

  const cached = cache.get(cacheKey);
  if (cached) {
    return res.json({
      ...cached,
      _fromCache: true
    });
  }

  const catalog = MovieScraperEngine.getCatalog(page);
  cache.set(cacheKey, catalog, CACHE_TTL_LIST);
  return res.json(catalog);
});

// GET /api/v1/extract?url=...
router.get('/extract', async (req, res) => {
  const movieUrl = req.query.url;
  if (!movieUrl) {
    return res.status(400).json({ status: 'error', message: 'Parámetro url es requerido' });
  }

  const cleanUrl = String(movieUrl).toLowerCase();
  const cacheKey = `extract_${cleanUrl}`;

  const cached = cache.get(cacheKey);
  if (cached) {
    return res.json({
      ...cached,
      _fromCache: true
    });
  }

  const foundMovie = SCRAPED_MOVIES.find(m =>
    cleanUrl.includes(m.url.toLowerCase()) ||
    cleanUrl.includes(encodeURIComponent(m.title.toLowerCase())) ||
    cleanUrl.includes(m.title.toLowerCase().replace(/\s+/g, '-')) ||
    m.url.toLowerCase().includes(cleanUrl)
  );

  if (!foundMovie) {
    return res.status(404).json({
      status: 'error',
      message: 'Aún no disponemos de enlaces para este contenido.',
      streams: [],
      streamsCount: 0
    });
  }

  const resolvedStreams = await MovieScraperEngine.resolveStreamsForMovie(foundMovie);

  const detailResponse = {
    status: 'success',
    title: foundMovie.title,
    rating: foundMovie.rating,
    tmdbRating: foundMovie.rating,
    description: foundMovie.description,
    poster: foundMovie.image,
    genres: foundMovie.genres,
    gallery: SCRAPED_MOVIES.slice(0, 4).map(m => m.image),
    streams: resolvedStreams,
    streamsCount: resolvedStreams.length,
    scrapedAt: new Date().toISOString()
  };

  cache.set(cacheKey, detailResponse, CACHE_TTL_EXTRACT);
  return res.json(detailResponse);
});

// GET /api/v1/search?q=...
router.get('/search', async (req, res) => {
  const query = String(req.query.q || '').trim();
  if (!query) {
    return res.json({ status: 'success', total: 0, movies: [] });
  }

  const searchResults = MovieScraperEngine.search(query);
  return res.json({
    status: 'success',
    query,
    total: searchResults.length,
    movies: searchResults
  });
});

// GET /api/v1/categories
router.get('/categories', async (_req, res) => {
  const categories = [
    { id: 'top10', name: 'Top 10 Hoy' },
    { id: 'latino', name: 'Español Latino' },
    { id: 'hq', name: 'Ultra HD 1080p / 60FPS' },
    { id: 'featured', name: 'Tendencias y Estrenos' },
    { id: 'action', name: 'Acción y Aventura' },
    { id: 'horror', name: 'Terror y Suspenso' },
    { id: 'comedy', name: 'Comedia' },
    { id: 'scifi', name: 'Ciencia Ficción' }
  ];

  return res.json({
    status: 'success',
    categories
  });
});

// GET /api/v1/health
router.get('/health', (_req, res) => {
  return res.json({
    status: 'online',
    service: 'APK-TVpro Multi-Source Scraper Gateway',
    timestamp: new Date().toISOString(),
    uptimeSeconds: Math.floor(process.uptime()),
    memoryUsageMB: Math.round(process.memoryUsage().rss / 1024 / 1024),
    activeScrapers: ['VidSrc Pro', 'SuperEmbed HD', 'SmashyStream', 'Runtime Live Cine', 'Popcorn Central HD'],
    cache: cache.getStats()
  });
});

export default router;
