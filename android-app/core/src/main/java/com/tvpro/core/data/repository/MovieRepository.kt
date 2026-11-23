package com.tvpro.core.data.repository

import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.model.*
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object MovieRepository {

    private val laMovieGenresMap = mapOf(
        8 to "Música", 17 to "Drama", 18 to "Comedia", 32 to "Acción",
        33 to "Suspenso", 96 to "Terror", 97 to "Misterio", 115 to "Romance",
        130 to "Aventura", 131 to "Ciencia Ficción", 164 to "Documental",
        165 to "Historia", 180 to "Crimen", 229 to "Fantasía", 398 to "Familia",
        520 to "Animación", 674 to "Western", 703 to "Infantil",
        704 to "Ciencia Ficción & Fantasía", 705 to "Acción & Aventura",
        786 to "Bélica", 3056 to "Bélica", 6787 to "Película de TV"
    )

    val localCatalog = listOf(
        MovieItem(
            title = "Avatar: El Camino del Agua",
            url = "https://tvpro.app/movies/avatar-el-camino-del-agua",
            image = "https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg",
            type = "featured",
            rating = "7.6",
            year = "2022",
            quality = "4K Ultra HD",
            languages = listOf("Latino", "Inglés Subtitulado"),
            overview = "Más de una década después, los hombres del cielo regresan a Pandora. Entonces Jake Sully, Neytiri y sus hijos deciden hacer un gran sacrificio para mantener a su pueblo a salvo y seguir con vida.",
            genres = listOf("Ciencia Ficción", "Aventura", "Acción")
        ),
        MovieItem(
            title = "Oppenheimer",
            url = "https://tvpro.app/movies/oppenheimer",
            image = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
            type = "featured",
            rating = "8.9",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino", "Inglés"),
            overview = "En tiempos de guerra, el físico estadounidense J. Robert Oppenheimer lidera el Proyecto Manhattan para construir la bomba atómica que cambiaría la historia.",
            genres = listOf("Drama", "Historia", "Suspenso")
        ),
        MovieItem(
            title = "Spider-Man: A Través del Spider-Verso",
            url = "https://tvpro.app/movies/spider-man-spider-verse",
            image = "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg",
            type = "featured",
            rating = "9.2",
            year = "2026",
            quality = "1080p 60 FPS",
            languages = listOf("Latino", "Castellano"),
            overview = "Tras reencontrarse con Gwen Stacy, el amigable vecino de tiempo completo de Brooklyn, Miles Morales, es catapultado a través del Multiverso, donde se encuentra con la Sociedad Arácnida, un equipo de Spider-Personas encargadas de proteger su propia existencia.",
            genres = listOf("Animación", "Acción", "Aventura", "Ciencia Ficción", "Familia")
        ),
        MovieItem(
            title = "Interestelar",
            url = "https://tvpro.app/movies/interstellar",
            image = "https://image.tmdb.org/t/p/w500/yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
            type = "normal",
            rating = "9.4",
            year = "2025",
            quality = "4K Ultra HD",
            languages = listOf("Latino", "Inglés"),
            overview = "Al ver que la vida en la Tierra está llegando a su fin, un grupo de exploradores dirigidos por el piloto Cooper y la científica Amelia emprenden una misión que puede ser la más importante de la historia de la humanidad: viajar más allá de nuestra galaxia.",
            genres = listOf("Ciencia Ficción", "Drama", "Aventura")
        ),
        MovieItem(
            title = "Duna: Parte Dos",
            url = "https://tvpro.app/movies/dune-parte-dos",
            image = "https://image.tmdb.org/t/p/w500/6izwz7rsy95ARzTR3poZ8H6c5pp.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
            type = "featured",
            rating = "8.8",
            year = "2026",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Paul Atreides se une a Chani y a los Fremen mientras emprende una guerra de venganza contra los conspiradores que destruyeron a su familia. Al enfrentarse a una elección entre el amor de su vida y el destino del universo, debe evitar un futuro terrible.",
            genres = listOf("Ciencia Ficción", "Aventura", "Acción")
        ),
        MovieItem(
            title = "Batman: El Caballero de la Noche",
            url = "https://tvpro.app/movies/batman-caballero-noche",
            image = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/k5r2N22yTSIlLZBraf9792MK1gN.jpg",
            type = "normal",
            rating = "9.0",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino", "Inglés"),
            overview = "Batman eleva las apuestas en su guerra contra el crimen. Con la ayuda del teniente Jim Gordon y el fiscal Harvey Dent, se propone desmantelar las organizaciones criminales de Gotham, pero pronto se ven acorralados por una mente maestra anárquica conocida como el Joker.",
            genres = listOf("Acción", "Crimen", "Drama", "Suspenso")
        ),
        MovieItem(
            title = "El Origen (Inception)",
            url = "https://tvpro.app/movies/el-origen",
            image = "https://image.tmdb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            type = "normal",
            rating = "8.8",
            year = "2025",
            quality = "1080p",
            languages = listOf("Latino"),
            overview = "Dom Cobb es un ladrón capaz de adentrarse en los sueños de la gente y hacerse con sus secretos más preciados. Sin embargo, esta vez su misión no será robar una idea, sino implantar una en el subconsciente de un poderoso heredero.",
            genres = listOf("Acción", "Ciencia Ficción", "Aventura", "Suspenso")
        ),
        MovieItem(
            title = "Gladiador II",
            url = "https://tvpro.app/movies/gladiador-2",
            image = "https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/euYIwmwkmz95mnXvufEmbL6ovhZ.jpg",
            type = "normal",
            rating = "8.7",
            year = "2026",
            quality = "4K Ultra HD",
            languages = listOf("Latino", "Castellano"),
            overview = "Años después de presenciar la muerte del venerado héroe Máximo a manos de su tío, Lucio se ve obligado a entrar en el Coliseo tras ser invadido su hogar por los tiránicos emperadores que ahora dirigen Roma con puño de hierro.",
            genres = listOf("Acción", "Aventura", "Drama", "Historia")
        ),
        MovieItem(
            title = "Blade Runner 2049",
            url = "https://tvpro.app/movies/blade-runner-2049",
            image = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/bMdSmfI0qwpAkvhAL7sqpjmwgf4.jpg",
            type = "normal",
            rating = "8.5",
            year = "2025",
            quality = "4K Ultra HD",
            languages = listOf("Latino", "Inglés"),
            overview = "Treinta años después de los eventos de la primera película, un nuevo blade runner, el oficial K de la policía de Los Ángeles, desentierra un viejo secreto que tiene el potencial de sumir en el caos lo que queda de la sociedad.",
            genres = listOf("Ciencia Ficción", "Drama", "Misterio")
        ),
        MovieItem(
            title = "The Matrix",
            url = "https://tvpro.app/movies/the-matrix",
            image = "https://image.tmdb.org/t/p/w500/dXNAPwY7VrqMAo51EKhhCJfaGb5.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/fNG7i7RqMErkcqhohV2a6cV1Ehy.jpg",
            type = "normal",
            rating = "8.7",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino"),
            overview = "Thomas Anderson lleva una doble vida: por el día es programador en una importante empresa de software, y por la noche un hacker informático llamado Neo. Su vida cambiará cuando Morfeo le descubra la verdadera realidad de Matrix.",
            genres = listOf("Acción", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Deadpool & Wolverine",
            url = "https://tvpro.app/movies/deadpool-wolverine",
            image = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/by8z9Fe8y7p4jo2YlW2SZDnptyT.jpg",
            type = "normal",
            rating = "8.6",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Un apático Wade Wilson se afana en la vida civil tras dejar atrás sus días como el mercenario moralmente flexible Deadpool. Pero cuando su mundo natal se enfrenta a una amenaza existencial, Wade debe volver a vestirse a regañadientes junto a Wolverine.",
            genres = listOf("Acción", "Comedia", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Furiosa: De la saga Mad Max",
            url = "https://tvpro.app/movies/furiosa",
            image = "https://image.tmdb.org/t/p/w500/iADOJ8Zymht2JPMoy3R7xceZprc.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/wNAhuOZ3Zf84jCIlrcI6JhgmY5q.jpg",
            type = "normal",
            rating = "8.3",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Mientras el mundo caía, la joven Furiosa es arrebatada del Lugar Verde de Muchas Madres y cae en manos de una gran Horda de Motoristas liderada por el Señor de la Guerra Dementus en su lucha por el dominio de la Ciudadela.",
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Alien: Romulus",
            url = "https://tvpro.app/movies/alien-romulus",
            image = "https://image.tmdb.org/t/p/w500/2uSWRTtCG336nuBiG8jOTEUKSy8.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/9SSEUrSqhljBMzRe4aBTh17rUaC.jpg",
            type = "normal",
            rating = "8.2",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Mientras rebuscan en las profundidades de una estación espacial abandonada, un grupo de jóvenes colonizadores del espacio se encuentra cara a cara con la forma de vida más aterradora del universo.",
            genres = listOf("Terror", "Ciencia Ficción", "Suspenso")
        ),
        MovieItem(
            title = "Godzilla y Kong: El Nuevo Imperio",
            url = "https://tvpro.app/movies/godzilla-kong",
            image = "https://image.tmdb.org/t/p/w500/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/veIyxxi5Gs8gvztLEW1Ysb8rrzs.jpg",
            type = "normal",
            rating = "8.1",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Una aventura cinematográfica en la que el todopoderoso Kong y el temible Godzilla se enfrentan a una colosal amenaza aún no descubierta oculta dentro de nuestro mundo, desafiando su propia existencia y la nuestra.",
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "El Planeta de los Simios: Nuevo Reino",
            url = "https://tvpro.app/movies/kingdom-planet-apes",
            image = "https://image.tmdb.org/t/p/w500/gKkl37BQuKTanygYQG1pyYgLVgf.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/fqv8v6AycXKsivp1T5yKtLbGXce.jpg",
            type = "normal",
            rating = "8.0",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Varias generaciones en el futuro tras el reinado de César, los simios son la especie dominante que vive en armonía y los humanos se han visto reducidos a vivir en la sombra. Mientras un nuevo líder tiránico construye su imperio, un joven simio emprende un angustioso viaje.",
            genres = listOf("Ciencia Ficción", "Aventura", "Acción")
        ),
        MovieItem(
            title = "Joker: Folie à Deux",
            url = "https://tvpro.app/movies/joker-2",
            image = "https://image.tmdb.org/t/p/w500/if8QiqCI7WAGImKcJCfzp6VTyKA.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/uGmYqxh8flqkudioyFtD7IJSHxK.jpg",
            type = "normal",
            rating = "7.8",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Arthur Fleck está institucionalizado en Arkham a la espera de juicio por sus crímenes como Joker. Mientras lucha con su doble identidad, Arthur no solo tropieza con el amor verdadero, sino que también encuentra la música que siempre ha estado dentro de él.",
            genres = listOf("Drama", "Crimen", "Suspenso", "Música")
        )
    )

    val localSeriesCatalog = listOf(
        MovieItem(
            title = "The Last of Us",
            url = "https://lamovie.org/series/the-last-of-us-2023",
            image = "https://lamovie.org/wp-content/uploads/thumbs/de8700d4f1a72631702e3f067dabf0b2_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/lY2DhbA7Hy44fAKddr06UrXWWaQ.jpg",
            type = "series",
            rating = "8.8",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Veinte años después de la destrucción de la civilización moderna a causa de un hongo —el cordyceps— que se adueña del cuerpo de los humanos, uno de los supervivientes, Joel, recibe el encargo de sacar a la joven Ellie de una opresiva zona de cuarentena. Juntos cruzan Estados Unidos ayudándose mutuamente para sobrevivir.",
            genres = listOf("Drama", "Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "La Casa del Dragón",
            url = "https://lamovie.org/series/house-of-the-dragon-2022",
            image = "https://image.tmdb.org/t/p/w500/7V0Ebks0GgpKvQ7QbLAIdX5dos4.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/etj8E2o0Bud0HkONVQPjyCkIvpv.jpg",
            type = "series",
            rating = "8.7",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Ambientada 200 años antes de los acontecimientos de Juego de Tronos, esta serie épica narra la historia de la Casa Targaryen y la sangrienta guerra civil de sucesión conocida como la Danza de los Dragones.",
            genres = listOf("Fantasía", "Drama", "Acción", "Aventura")
        ),
        MovieItem(
            title = "Shōgun",
            url = "https://lamovie.org/series/shogun-2024",
            image = "https://lamovie.org/wp-content/uploads/thumbs/e4825ad3aaa7b29257a3193ff024aa83_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/bwSmgmd90hCWwqOKQYTEraeOZhJ.jpg",
            type = "series",
            rating = "8.8",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Ambientada en el Japón de 1600, en los albores de una guerra civil que definirá un siglo, Lord Yoshii Toranaga lucha por su vida mientras sus enemigos en el Consejo de Regentes se unen contra él, cuando un misterioso barco europeo aparece abandonado en un pueblo pesquero cercano.",
            genres = listOf("Drama", "Historia", "Acción", "Bélica")
        ),
        MovieItem(
            title = "Fallout",
            url = "https://lamovie.org/series/fallout-2024",
            image = "https://lamovie.org/wp-content/uploads/thumbs/ad32ace768f856e315a43bfc3f88f4d2_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/5C3UcPKVcg3rwGBYPE18wXpZMw8.jpg",
            type = "series",
            rating = "8.6",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Doscientos años después del apocalipsis nuclear, los pacíficos habitantes de los lujosos refugios subterráneos se ven obligados a regresar al paisaje infernal cargado de radiación que sus antepasados dejaron atrás, descubriendo un universo complejo, extraño y sumamente violento.",
            genres = listOf("Ciencia Ficción", "Acción", "Aventura", "Comedia")
        ),
        MovieItem(
            title = "The Boys",
            url = "https://lamovie.org/series/the-boys-2019",
            image = "https://lamovie.org/wp-content/uploads/thumbs/bb31bb1b1b3b1900fa619d1a7e3ebb92_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/7cqKGQMnNabzOpi7qaIgZvQ7NGV.jpg",
            type = "series",
            rating = "8.7",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino"),
            overview = "La serie tiene lugar en un mundo en el que los superhéroes representan el lado oscuro de la celebridad y la fama. Un grupo de vigilantes que se hacen llamar 'The Boys' decide hacer todo lo posible por frenar a los superhéroes corruptos que abusan de sus superpoderes.",
            genres = listOf("Acción", "Ciencia Ficción", "Comedia", "Crimen")
        ),
        MovieItem(
            title = "Stranger Things",
            url = "https://lamovie.org/series/stranger-things-2016",
            image = "https://image.tmdb.org/t/p/w500/uOOtwVbSr4QDjAGIifLDwpb2Pdl.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
            type = "series",
            rating = "8.7",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "A raíz de la desaparición de un niño, un pueblo desvela un misterio relacionado con experimentos secretos, fuerzas sobrenaturales aterradoras y una niña muy extraña.",
            genres = listOf("Ciencia Ficción", "Fantasía", "Misterio", "Drama")
        ),
        MovieItem(
            title = "Loki",
            url = "https://lamovie.org/series/loki-2021",
            image = "https://lamovie.org/wp-content/uploads/thumbs/8e8f712513c17b5f8e30ad6c04624f51_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/q3jHCb4dMfYF6ojikKuHd6LscxC.jpg",
            type = "series",
            rating = "8.4",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Loki, el Dios de las Mentiras, se aleja de la sombra de su hermano para embarcarse en una aventura que se desarrolla tras los acontecimientos de Vengadores: Endgame junto a la Autoridad de Variación Temporal.",
            genres = listOf("Ciencia Ficción", "Fantasía", "Acción", "Aventura")
        ),
        MovieItem(
            title = "Breaking Bad",
            url = "https://lamovie.org/series/breaking-bad-2008",
            image = "https://lamovie.org/wp-content/uploads/thumbs/0966289037ad9846c5e994be2a91bafa_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg",
            type = "series",
            rating = "9.5",
            year = "2023",
            quality = "1080p",
            languages = listOf("Latino"),
            overview = "Tras cumplir 50 años, Walter White, un profesor de química de un instituto de Albuquerque, Nuevo México, se entera de que tiene un cáncer de pulmón incurable y se adentra en el peligroso mundo del narcotráfico para asegurar el futuro de su familia.",
            genres = listOf("Drama", "Crimen", "Suspenso")
        ),
        MovieItem(
            title = "Severance",
            url = "https://lamovie.org/series/severance-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/67fabd9dad200fe4c419fcb14cb61c25_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/ixgFmf1X59PUZam2qbAfskx2gQr.jpg",
            type = "series",
            rating = "8.7",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Mark dirige un equipo de oficinistas cuyos recuerdos han sido divididos quirúrgicamente entre su trabajo y su vida personal. Cuando un misterioso colega aparece fuera del trabajo, comienza un viaje para descubrir la verdad sobre su empleo.",
            genres = listOf("Drama", "Misterio", "Ciencia Ficción", "Suspenso")
        ),
        MovieItem(
            title = "Juego de Tronos",
            url = "https://lamovie.org/series/juego-de-tronos-2011",
            image = "https://lamovie.org/wp-content/uploads/thumbs/602d1305678a8d5fdb372271e980da6a_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/zZqpAXxVSBtxV9qPBcscfXBcL2w.jpg",
            type = "series",
            rating = "9.2",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "En un mundo fantástico y en un contexto medieval varias familias, relativas a la nobleza, se disputan el poder para dominar el territorio ficticio de Poniente (Westeros) y tomar el control de los Siete Reinos desde el Trono de Hierro.",
            genres = listOf("Fantasía", "Drama", "Acción", "Aventura")
        ),
        MovieItem(
            title = "Peaky Blinders",
            url = "https://lamovie.org/series/peaky-blinders-2013",
            image = "https://lamovie.org/wp-content/uploads/thumbs/58b481d9dbc5b5b2fdabb5d12dece1a9_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/l8v3gJDlASN0lNn51gR8zQJsu5O.jpg",
            type = "series",
            rating = "8.8",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Una familia de gánsteres asentada en Birmingham tras la Primera Guerra Mundial dirige un local de apuestas hípicas, mientras su ambicioso jefe, Tommy Shelby, expande su imperio criminal.",
            genres = listOf("Drama", "Crimen", "Historia")
        ),
        MovieItem(
            title = "Vikingos",
            url = "https://lamovie.org/series/vikingos-valhalla-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/aacb2e3e4af5e4972379d84e9b6430e9_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/k47JEUTQsSMN532HRg6RCzZKBdB.jpg",
            type = "series",
            rating = "8.6",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Cien años después de las hazañas del legendario Ragnar Lothbrok, una nueva generación de héroes legendarios surge para forjar su propio destino y hacer historia.",
            genres = listOf("Acción", "Aventura", "Drama", "Historia")
        ),
        MovieItem(
            title = "Chernobyl",
            url = "https://lamovie.org/series/chernobyl-2019",
            image = "https://lamovie.org/wp-content/uploads/thumbs/dcc6a3c53c062a25e84bf4397a4f8d26_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/900tHlUYUkp7Ol04XFSoAaEIXcT.jpg",
            type = "series",
            rating = "9.4",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Sigue la verdadera historia de una de las peores catástrofes provocadas por el hombre y habla de los valientes hombres y mujeres que se sacrificaron para salvar a Europa de un desastre inimaginable.",
            genres = listOf("Drama", "Historia", "Suspenso")
        ),
        MovieItem(
            title = "Dark",
            url = "https://lamovie.org/series/dark-2017",
            image = "https://image.tmdb.org/t/p/w500/eRtnI7qTJIiPUmlY3J8TTM3jlT7.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/3lBDg3i6nn5R2NKFCJ6oKyUo2j5.jpg",
            type = "series",
            rating = "8.7",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            overview = "Tras la desaparición de un joven, cuatro familias desesperadas tratan de entender lo ocurrido a medida que van desvelando un retorcido misterio que abarca tres generaciones.",
            genres = listOf("Ciencia Ficción", "Misterio", "Drama", "Suspenso")
        )
    )

    val localAnimeCatalog = listOf(
        MovieItem(
            title = "Solo Leveling",
            url = "https://lamovie.org/animes/solo-leveling-2024",
            image = "https://lamovie.org/wp-content/uploads/thumbs/28d0176d7f64a91d96a0228f774e39e4_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/odVlTMqPPiMksmxpN9cCbPCjUPP.jpg",
            type = "anime",
            rating = "8.8",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino", "Japonés Sub"),
            overview = "Lo que no te mata te hace más fuerte, pero en el caso de Sung Jinwoo, lo que lo mató lo hizo más fuerte. Después de ser brutalmente asesinado por monstruos en una mazmorra de alto rango, Jinwoo regresó con el Sistema, un programa que solo él puede ver y que eleva su nivel en todos los sentidos.",
            genres = listOf("Acción", "Aventura", "Fantasía", "Animación")
        ),
        MovieItem(
            title = "Demon Slayer: Kimetsu no Yaiba",
            url = "https://tvpro.app/anime/demon-slayer",
            image = "https://image.tmdb.org/t/p/w500/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/nTvM4mhqNlHIvUkI1gVnW6XP7GG.jpg",
            type = "anime",
            rating = "8.9",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino", "Japonés Sub"),
            overview = "En la era Taisho en Japón, Tanjiro Kamado es un joven bondadoso que tras encontrar a su familia masacrada por un demonio y a su hermana Nezuko convertida en uno, se une al Cuerpo de Exterminio de Demonios para hallar una cura.",
            genres = listOf("Acción", "Fantasía", "Aventura", "Animación")
        ),
        MovieItem(
            title = "Jujutsu Kaisen",
            url = "https://tvpro.app/anime/jujutsu-kaisen",
            image = "https://image.tmdb.org/t/p/w500/6qQzMJG27XOJsyAEEIisoJB45j2.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/gmECX1DvFgdUPjtio2zaL8BPYPu.jpg",
            type = "anime",
            rating = "8.8",
            year = "2024",
            quality = "1080p 60 FPS",
            languages = listOf("Latino", "Japonés Sub"),
            overview = "Yuji Itadori traga un dedo maldito para salvar a sus compañeros de club y ahora comparte cuerpo con Ryomen Sukuna, el Rey de las Maldiciones. Guiado por Satoru Gojo, ingresa a la Escuela Técnica de Hechicería de Tokio.",
            genres = listOf("Acción", "Fantasía", "Suspenso", "Animación")
        ),
        MovieItem(
            title = "Attack on Titan",
            url = "https://lamovie.org/animes/attack-on-titan-2013",
            image = "https://lamovie.org/wp-content/uploads/thumbs/56352739f59643540a3a6e16985f62c7_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/aok7IhrbA83josNz9Dqh8tNA0Ao.jpg",
            type = "anime",
            rating = "9.1",
            year = "2023",
            quality = "1080p",
            languages = listOf("Latino"),
            overview = "Cien años atrás, gigantescos titanes llevaron a la humanidad al borde de la extinción, obligando a los sobrevivientes a refugiarse tras tres enormes murallas. Cuando el Titán Colosal derriba el muro exterior, Eren Jaeger jura exterminarlos a todos.",
            genres = listOf("Acción", "Drama", "Fantasía", "Misterio")
        ),
        MovieItem(
            title = "Chainsaw Man",
            url = "https://lamovie.org/animes/chainsaw-man-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/649baac3b3b053944420e3a069e8949a_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/5DUMPBSnHOZsbBv81GFXZXvDpo6.jpg",
            type = "anime",
            rating = "8.6",
            year = "2023",
            quality = "1080p",
            languages = listOf("Latino"),
            overview = "Denji es un adolescente que vive con un demonio motosierra llamado Pochita. Para pagar la deuda que le dejó su padre, trabaja cazando demonios hasta que una traición lo lleva a fusionarse con Pochita y renacer como Chainsaw Man.",
            genres = listOf("Acción", "Fantasía", "Terror", "Comedia")
        ),
        MovieItem(
            title = "One Piece",
            url = "https://lamovie.org/animes/one-piece-1999",
            image = "https://lamovie.org/wp-content/uploads/thumbs/7b9226c896ac6346c8f2087125d65704_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/2rmK7mnchw9Xr3XdiTFSxTTLXqv.jpg",
            type = "anime",
            rating = "9.0",
            year = "2024",
            quality = "1080p",
            languages = listOf("Latino"),
            overview = "Monkey D. Luffy se niega a que nadie se interponga en su camino para convertirse en el Rey de los Piratas. Con un cuerpo elástico tras comer la Fruta Gomu Gomu, zarpa hacia el Grand Line en busca del legendario tesoro One Piece.",
            genres = listOf("Acción", "Aventura", "Comedia", "Fantasía")
        ),
        MovieItem(
            title = "Dragon Ball DAIMA",
            url = "https://lamovie.org/animes/dragon-ball-daima-2024",
            image = "https://lamovie.org/wp-content/uploads/thumbs/2c65a29c8f30f06d5ef3a0dacaf028db_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/oUmWLyeko3kYdUr8DBLIsxwcugl.jpg",
            type = "anime",
            rating = "8.4",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Debido a una conspiración misteriosa, Goku y sus amigos son convertidos en niños pequeños. Para revertir esta situación, deberán adentrarse en un nuevo mundo desconocido lleno de desafíos en el Reino de los Demonios.",
            genres = listOf("Acción", "Aventura", "Fantasía", "Comedia")
        ),
        MovieItem(
            title = "Naruto Shippūden",
            url = "https://lamovie.org/animes/naruto-shippuden-2007",
            image = "https://lamovie.org/wp-content/uploads/thumbs/29a3d252405fb67dcf7e17e04522fff0_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/nAeS7F2B22XQOLh5HVNXn8G3ZRk.jpg",
            type = "anime",
            rating = "8.7",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Dos años y medio después de partir de la Aldea Oculta de la Hoja para entrenar con Jiraiya, Naruto Uzumaki regresa más fuerte que nunca mientras la misteriosa organización Akatsuki comienza su plan para capturar a las Bestias con Cola.",
            genres = listOf("Acción", "Aventura", "Fantasía", "Drama")
        ),
        MovieItem(
            title = "Death Note",
            url = "https://lamovie.org/animes/death-note-2006",
            image = "https://lamovie.org/wp-content/uploads/thumbs/8bb74b945ed32ca2b2f8da600e247d20_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/koBa94NxTaGe2FoDzlXMuJptsv0.jpg",
            type = "anime",
            rating = "8.9",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Light Yagami es un brillante estudiante japonés que encuentra un cuaderno sobrenatural llamado Death Note, perteneciente al Shinigami Ryuk, capaz de matar a cualquier persona cuyo nombre sea escrito en sus páginas.",
            genres = listOf("Misterio", "Suspenso", "Crimen", "Drama")
        ),
        MovieItem(
            title = "Bleach",
            url = "https://lamovie.org/animes/bleach-2004",
            image = "https://lamovie.org/wp-content/uploads/thumbs/9379824b37bdf1d40d517e0a8e4ea024_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/o0NsbcIvsllg6CJX0FBFY8wWbsn.jpg",
            type = "anime",
            rating = "8.8",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "Ichigo Kurosaki es un adolescente capaz de ver fantasmas cuya vida cambia radicalmente cuando conoce a Rukia Kuchiki, una Shinigami que se ve obligada a transferirle sus poderes para proteger a su familia de los Hollows.",
            genres = listOf("Acción", "Aventura", "Fantasía")
        ),
        MovieItem(
            title = "SPY x FAMILY",
            url = "https://lamovie.org/animes/spy-x-family-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/bef780abbcaccd7725caadf59d98df66_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/lysUnU6V0VfcthDbviuVlIqgHOR.jpg",
            type = "anime",
            rating = "8.6",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "El espía de élite Twilight debe formar una familia ficticia para infiltrarse en un prestigioso colegio, sin saber que la niña que adopta es una telépata y la mujer con la que acepta fingir un matrimonio es una letal asesina a sueldo.",
            genres = listOf("Comedia", "Acción", "Familia", "Animación")
        ),
        MovieItem(
            title = "My Hero Academia",
            url = "https://lamovie.org/animes/my-hero-academia-2016",
            image = "https://lamovie.org/wp-content/uploads/thumbs/c4c4314e940211ff8ccb65ab30e4d364_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/6wSSyRNeF9vBJ1h29nuw3vG9hZI.jpg",
            type = "anime",
            rating = "8.4",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            overview = "En un mundo donde el 80% de la población posee superpoderes llamados Dones, Izuku Midoriya nace sin ninguno, pero sueña con convertirse en un gran héroe hasta que conoce a su ídolo, el legendario All Might.",
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        )
    )

    val localDramaCatalog = listOf(
        MovieItem(
            title = "La Reina de las Lágrimas",
            url = "https://lamovie.org/series/queen-of-tears-2024",
            image = "https://image.tmdb.org/t/p/w500/7ZXLZ3KYL3IVvsSHBZaHjcNQzNU.jpg",
            type = "drama",
            rating = "8.7",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Romance", "Comedia")
        ),
        MovieItem(
            title = "Aterrizaje de emergencia en tu corazón",
            url = "https://tvpro.app/novelas/crash-landing-on-you",
            image = "https://image.tmdb.org/t/p/w500/fgBNLPr6mC8pxuR79ENAJY4nBmj.jpg",
            type = "drama",
            rating = "8.8",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Romance", "Comedia")
        ),
        MovieItem(
            title = "Betty la Fea: La historia continúa",
            url = "https://tvpro.app/novelas/betty-la-fea-continua",
            image = "https://image.tmdb.org/t/p/w500/huEeQQBmZsZ5qDXkWl0pS5vPWIU.jpg",
            type = "drama",
            rating = "8.2",
            year = "2024",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Comedia", "Drama", "Romance")
        ),
        MovieItem(
            title = "Pasión de Gavilanes",
            url = "https://tvpro.app/novelas/pasion-de-gavilanes",
            image = "https://image.tmdb.org/t/p/w500/91UV7pNcDPhIzJl7EuK36sK5vRG.jpg",
            type = "drama",
            rating = "8.0",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Romance")
        ),
        MovieItem(
            title = "Café con aroma de mujer",
            url = "https://tvpro.app/novelas/cafe-aroma-mujer",
            image = "https://image.tmdb.org/t/p/w500/51sT2YMf3lmJpFLcYUfpFA0prgc.jpg",
            type = "drama",
            rating = "7.9",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Romance")
        ),
        MovieItem(
            title = "La Reina del Flow",
            url = "https://lamovie.org/series/la-reina-del-flow-2018",
            image = "https://image.tmdb.org/t/p/w500/fuVuDYrs8sxvEolnYr0wCSvtyTi.jpg",
            type = "drama",
            rating = "8.2",
            year = "2023",
            quality = "Full HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Música", "Romance")
        )
    )

    data class MovieCollectionSection(
        val id: String,
        val title: String,
        val movies: List<MovieItem>
    )

    val mcuCollectionMovies = listOf(
        MovieItem(
            title = "Avengers: Endgame",
            url = "https://lamovie.org/peliculas/avengers-endgame-2019",
            image = "https://lamovie.org/wp-content/uploads/thumbs/9502b6ff74f460718dad211ae6d4ae1e_hd.webp",
            type = "normal",
            rating = "8.4",
            year = "2019",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "Avengers: Infinity War",
            url = "https://lamovie.org/peliculas/avengers-infinity-war-2018",
            image = "https://lamovie.org/wp-content/uploads/thumbs/fcc0283d66c53a7701d3786ba5d08b3b_hd.webp",
            type = "normal",
            rating = "8.4",
            year = "2018",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "Deadpool & Wolverine",
            url = "https://tvpro.app/movies/deadpool-wolverine",
            image = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/by8z9Fe8y7p4jo2YlW2SZDnptyT.jpg",
            type = "normal",
            rating = "8.6",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Comedia", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Spider-Man: Sin Camino a Casa",
            url = "https://lamovie.org/peliculas/spider-man-sin-camino-a-casa-2021",
            image = "https://lamovie.org/wp-content/uploads/thumbs/99d364e41030613ecea8d8fc29d85873_hd.webp",
            type = "normal",
            rating = "8.2",
            year = "2021",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Guardianes de la Galaxia Vol. 3",
            url = "https://lamovie.org/peliculas/guardianes-de-la-galaxia-volumen-3-2023",
            image = "https://lamovie.org/wp-content/uploads/thumbs/52012d28d631ee433533c0bb4260b475_hd.webp",
            type = "normal",
            rating = "8.0",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Ciencia Ficción", "Aventura", "Acción")
        ),
        MovieItem(
            title = "Capitán América: Civil War",
            url = "https://lamovie.org/peliculas/capitan-america-civil-war-2016",
            image = "https://lamovie.org/wp-content/uploads/thumbs/d0078e85598bdf64517f1ebeaeb3537a_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2016",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Acción", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Avengers: Los Vengadores",
            url = "https://lamovie.org/peliculas/avengers-los-vengadores-2012",
            image = "https://lamovie.org/wp-content/uploads/thumbs/07e99f116b916656b3fd4f7dd58a3cbe_hd.webp",
            type = "normal",
            rating = "8.0",
            year = "2012",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Ciencia Ficción", "Acción", "Aventura")
        ),
        MovieItem(
            title = "Iron Man: El Hombre de Hierro",
            url = "https://lamovie.org/peliculas/iron-man-el-hombre-de-hierro-2008",
            image = "https://lamovie.org/wp-content/uploads/thumbs/74563ba21a90da13dacf2a73e3ddefa7_hd.webp",
            type = "normal",
            rating = "7.9",
            year = "2008",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "Capitán América y el Soldado del Invierno",
            url = "https://lamovie.org/peliculas/capitan-america-y-el-soldado-del-invierno-2014",
            image = "https://lamovie.org/wp-content/uploads/thumbs/0c9de7fbac44fdcea080c61e55772199_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2014",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Thor: Ragnarok",
            url = "https://lamovie.org/peliculas/thor-ragnarok-2017",
            image = "https://lamovie.org/wp-content/uploads/thumbs/454b3b867e38d72fdb1f2ed6143822c3_hd.webp",
            type = "normal",
            rating = "7.9",
            year = "2017",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Comedia")
        ),
        MovieItem(
            title = "Pantera Negra",
            url = "https://lamovie.org/peliculas/pantera-negra-2018",
            image = "https://lamovie.org/wp-content/uploads/thumbs/2ea6eb875e378ab540a4e598c6ebe98e_hd.webp",
            type = "normal",
            rating = "7.4",
            year = "2018",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Doctor Strange en el Multiverso de la Locura",
            url = "https://lamovie.org/peliculas/doctor-strange-en-el-multiverso-de-la-locura-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/296aabf0168a24e3cd00ca57cb7bc44b_hd.webp",
            type = "normal",
            rating = "7.3",
            year = "2022",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Fantasía", "Acción", "Aventura")
        ),
        MovieItem(
            title = "Guardianes de la Galaxia",
            url = "https://lamovie.org/peliculas/guardianes-de-la-galaxia-2014",
            image = "https://lamovie.org/wp-content/uploads/thumbs/a098e41f026d8a5deb0c243f4120a958_hd.webp",
            type = "normal",
            rating = "7.9",
            year = "2014",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "Avengers: Era de Ultrón",
            url = "https://lamovie.org/peliculas/avengers-2-era-de-ultron-2015",
            image = "https://lamovie.org/wp-content/uploads/thumbs/457c074f6821cff7820fef54319dde53_hd.webp",
            type = "normal",
            rating = "7.3",
            year = "2015",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Spider-Man: Lejos de Casa",
            url = "https://lamovie.org/peliculas/spider-man-2-lejos-de-casa-2019",
            image = "https://lamovie.org/wp-content/uploads/thumbs/c1259936a8728d7687b7e7ad85bc1e67_hd.webp",
            type = "normal",
            rating = "7.4",
            year = "2019",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Spider-Man: De Regreso a Casa",
            url = "https://lamovie.org/peliculas/spider-man-de-regreso-a-casa-2017",
            image = "https://lamovie.org/wp-content/uploads/thumbs/e2d86a2d5ba5368e086858f56bcb5533_hd.webp",
            type = "normal",
            rating = "7.4",
            year = "2017",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Doctor Strange: Hechicero Supremo",
            url = "https://lamovie.org/peliculas/doctor-strange-hechicero-supremo-2016",
            image = "https://lamovie.org/wp-content/uploads/thumbs/29c140acc3f6491a6076a65d07de696f_hd.webp",
            type = "normal",
            rating = "7.5",
            year = "2016",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Fantasía")
        ),
        MovieItem(
            title = "Pantera Negra: Wakanda por Siempre",
            url = "https://lamovie.org/peliculas/pantera-negra-wakanda-por-siempre-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/5d4824887fc4c23c94e35fdef79ecd6a_hd.webp",
            type = "normal",
            rating = "7.2",
            year = "2022",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Ciencia Ficción")
        )
    )

    val harryPotterCollectionMovies = listOf(
        MovieItem(
            title = "Harry Potter y la Piedra Filosofal",
            url = "https://lamovie.org/peliculas/harry-potter-y-la-piedra-filosofal-2001",
            image = "https://lamovie.org/wp-content/uploads/thumbs/5dd9db5e033da9c6fb5ba83c7a7ebea9_hd.webp",
            type = "normal",
            rating = "7.9",
            year = "2001",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y la Cámara Secreta",
            url = "https://lamovie.org/peliculas/harry-potter-y-la-camara-secreta-2002",
            image = "https://lamovie.org/wp-content/uploads/thumbs/2dea61eed4bceec564a00115c4d21334_hd.webp",
            type = "normal",
            rating = "7.7",
            year = "2002",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y el Prisionero de Azkaban",
            url = "https://lamovie.org/peliculas/harry-potter-y-el-prisionero-de-azkaban-2004",
            image = "https://lamovie.org/wp-content/uploads/thumbs/9f396fe44e7c05c16873b05ec425cbad_hd.webp",
            type = "normal",
            rating = "8.0",
            year = "2004",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y el Cáliz de Fuego",
            url = "https://lamovie.org/peliculas/harry-potter-y-el-caliz-de-fuego-2005",
            image = "https://lamovie.org/wp-content/uploads/thumbs/0d7de1aca9299fe63f3e0041f02638a3_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2005",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y la Orden del Fénix",
            url = "https://lamovie.org/peliculas/harry-potter-y-la-orden-del-fenix-2007",
            image = "https://lamovie.org/wp-content/uploads/thumbs/8fecb20817b3847419bb3de39a609afe_hd.webp",
            type = "normal",
            rating = "7.7",
            year = "2007",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Misterio")
        ),
        MovieItem(
            title = "Harry Potter y el Misterio del Príncipe",
            url = "https://lamovie.org/peliculas/harry-potter-y-el-misterio-del-principe-2009",
            image = "https://lamovie.org/wp-content/uploads/thumbs/f2201f5191c4e92cc5af043eebfd0946_hd.webp",
            type = "normal",
            rating = "7.7",
            year = "2009",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y las Reliquias de la Muerte: Parte 1",
            url = "https://lamovie.org/peliculas/harry-potter-y-las-reliquias-de-la-muerte-1a-parte-2010",
            image = "https://lamovie.org/wp-content/uploads/thumbs/b7cf1dbb51db462f3ad750d65d42a3a6_hd.webp",
            type = "normal",
            rating = "7.7",
            year = "2010",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Familia")
        ),
        MovieItem(
            title = "Harry Potter y las Reliquias de la Muerte: Parte 2",
            url = "https://lamovie.org/peliculas/harry-potter-y-las-reliquias-de-la-muerte-parte-2-2011",
            image = "https://lamovie.org/wp-content/uploads/thumbs/571ac6dcf08ffecf9796cb9706c75eec_hd.webp",
            type = "normal",
            rating = "8.1",
            year = "2011",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Fantasía", "Aventura", "Acción")
        ),
        MovieItem(
            title = "Animales Fantásticos y Dónde Encontrarlos",
            url = "https://lamovie.org/peliculas/animales-fantasticos-y-donde-encontrarlos-2016",
            image = "https://lamovie.org/wp-content/uploads/thumbs/9f525c64546fece536b65404f2094dac_hd.webp",
            type = "normal",
            rating = "7.3",
            year = "2016",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Fantasía", "Aventura", "Familia")
        ),
        MovieItem(
            title = "Animales Fantásticos: Los Crímenes de Grindelwald",
            url = "https://lamovie.org/peliculas/animales-fantasticos-2-los-crimenes-de-grindelwald-2018",
            image = "https://lamovie.org/wp-content/uploads/thumbs/b46c472594a3f40a1c2cd6a5a0293b36_hd.webp",
            type = "normal",
            rating = "6.8",
            year = "2018",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Fantasía", "Aventura", "Drama")
        ),
        MovieItem(
            title = "Animales Fantásticos: Los Secretos de Dumbledore",
            url = "https://lamovie.org/peliculas/animales-fantasticos-3-los-secretos-de-dumbledore-2022",
            image = "https://lamovie.org/wp-content/uploads/thumbs/6a43e3f8805698ba8b3f1b8c86efd23d_hd.webp",
            type = "normal",
            rating = "6.8",
            year = "2022",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Fantasía", "Aventura", "Acción")
        )
    )

    val lotrCollectionMovies = listOf(
        MovieItem(
            title = "El Señor de los Anillos: La Comunidad del Anillo",
            url = "https://lamovie.org/peliculas/el-senor-de-los-anillos-la-comunidad-del-anillo-2001",
            image = "https://lamovie.org/wp-content/uploads/thumbs/da4fb5c6e93e74d3df8527599fa62642_hd.webp",
            type = "normal",
            rating = "8.9",
            year = "2001",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Acción")
        ),
        MovieItem(
            title = "El Señor de los Anillos: Las Dos Torres",
            url = "https://lamovie.org/peliculas/el-senor-de-los-anillos-las-dos-torres-2002",
            image = "https://lamovie.org/wp-content/uploads/thumbs/4c56ff4ce4aaf9573aa5dff913df997a_hd.webp",
            type = "normal",
            rating = "8.8",
            year = "2002",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Acción")
        ),
        MovieItem(
            title = "El Señor de los Anillos: El Retorno del Rey",
            url = "https://lamovie.org/peliculas/el-senor-de-los-anillos-el-retorno-del-rey-2003",
            image = "https://lamovie.org/wp-content/uploads/thumbs/a0a080f42e6f13b3a2df133f073095dd_hd.webp",
            type = "normal",
            rating = "9.0",
            year = "2003",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Acción")
        ),
        MovieItem(
            title = "El Hobbit: Un Viaje Inesperado",
            url = "https://lamovie.org/peliculas/el-hobbit-un-viaje-inesperado-2012",
            image = "https://lamovie.org/wp-content/uploads/thumbs/e13963f0c129b9da11f1e4721f423745_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2012",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Acción")
        ),
        MovieItem(
            title = "El Hobbit: La Desolación de Smaug",
            url = "https://lamovie.org/peliculas/el-hobbit-2-la-desolacion-de-smaug-2013",
            image = "https://lamovie.org/wp-content/uploads/thumbs/db64fae6ec5e29854f3ac5b079a0b47d_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2013",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Aventura", "Fantasía", "Acción")
        ),
        MovieItem(
            title = "El Hobbit: La Batalla de los Cinco Ejércitos",
            url = "https://lamovie.org/peliculas/el-hobbit-3-la-batalla-de-los-cinco-ejercitos-2014",
            image = "https://lamovie.org/wp-content/uploads/thumbs/97bc031258b897d7de4570d05d138548_hd.webp",
            type = "normal",
            rating = "7.4",
            year = "2014",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Aventura", "Fantasía")
        ),
        MovieItem(
            title = "El Señor de los Anillos: La Guerra de los Rohirrim",
            url = "https://lamovie.org/peliculas/el-senor-de-los-anillos-la-guerra-de-los-rohirrim-2024",
            image = "https://lamovie.org/wp-content/uploads/thumbs/a06fef5a86fd5f20ff06185f27d963ba_hd.webp",
            type = "normal",
            rating = "7.2",
            year = "2024",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Animación", "Fantasía", "Acción", "Aventura")
        )
    )

    val nolanCollectionMovies = listOf(
        MovieItem(
            title = "Oppenheimer",
            url = "https://tvpro.app/movies/oppenheimer",
            image = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
            type = "normal",
            rating = "9.1",
            year = "2023",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Historia", "Suspenso")
        ),
        MovieItem(
            title = "Interestelar",
            url = "https://tvpro.app/movies/interstellar",
            image = "https://image.tmdb.org/t/p/w500/yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
            type = "normal",
            rating = "9.4",
            year = "2014",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Ciencia Ficción", "Drama", "Aventura")
        ),
        MovieItem(
            title = "El Origen (Inception)",
            url = "https://tvpro.app/movies/el-origen",
            image = "https://image.tmdb.org/t/p/w500/xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg",
            backdrop = "https://image.tmdb.org/t/p/original/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            type = "normal",
            rating = "8.8",
            year = "2010",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Ciencia Ficción", "Aventura")
        ),
        MovieItem(
            title = "Batman: El Caballero de la Noche",
            url = "https://lamovie.org/peliculas/batman-el-caballero-de-la-noche-2008",
            image = "https://lamovie.org/wp-content/uploads/thumbs/2a79ea27c279e471f4d180b08d62b00a_hd.webp",
            backdrop = "https://image.tmdb.org/t/p/original/k5r2N22yTSIlLZBraf9792MK1gN.jpg",
            type = "normal",
            rating = "9.0",
            year = "2008",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Crimen", "Drama", "Suspenso")
        ),
        MovieItem(
            title = "Batman: El Caballero de la Noche Asciende",
            url = "https://lamovie.org/peliculas/batman-el-caballero-de-la-noche-asciende-2012",
            image = "https://lamovie.org/wp-content/uploads/thumbs/187d2a7a5a7fef62a52fb0721e5ab448_hd.webp",
            type = "normal",
            rating = "8.4",
            year = "2012",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Crimen", "Drama", "Suspenso")
        ),
        MovieItem(
            title = "Batman Inicia",
            url = "https://lamovie.org/peliculas/batman-inicia-2005",
            image = "https://lamovie.org/wp-content/uploads/thumbs/7a614fd06c325499f1680b9896beedeb_hd.webp",
            type = "normal",
            rating = "8.2",
            year = "2005",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Crimen", "Drama")
        ),
        MovieItem(
            title = "El Gran Truco (The Prestige)",
            url = "https://lamovie.org/peliculas/el-gran-truco-2006",
            image = "https://lamovie.org/wp-content/uploads/thumbs/c7635bfd99248a2cdef8249ef7bfbef4_hd.webp",
            type = "normal",
            rating = "8.5",
            year = "2006",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Drama", "Misterio", "Ciencia Ficción")
        ),
        MovieItem(
            title = "Dunkerque",
            url = "https://lamovie.org/peliculas/dunkerque-2017",
            image = "https://lamovie.org/wp-content/uploads/thumbs/ad78fc7481a7eac263d779dbb1c72aff_hd.webp",
            type = "normal",
            rating = "7.8",
            year = "2017",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Bélica", "Acción", "Drama", "Historia")
        ),
        MovieItem(
            title = "Tenet",
            url = "https://lamovie.org/peliculas/tenet-2020",
            image = "https://lamovie.org/wp-content/uploads/thumbs/cd042f98360cd4dc25b19b273bfc4cd0_hd.webp",
            type = "normal",
            rating = "7.4",
            year = "2020",
            quality = "4K Ultra HD",
            languages = listOf("Latino"),
            genres = listOf("Acción", "Suspenso", "Ciencia Ficción")
        )
    )

    val featuredCollections: List<MovieCollectionSection> = listOf(
        MovieCollectionSection(
            id = "mcu",
            title = "Universo Cinematográfico de Marvel (UCM)",
            movies = mcuCollectionMovies
        ),
        MovieCollectionSection(
            id = "harry_potter",
            title = "Saga Harry Potter y el Mundo Mágico",
            movies = harryPotterCollectionMovies
        ),
        MovieCollectionSection(
            id = "lotr",
            title = "Tierra Media: El Señor de los Anillos y El Hobbit",
            movies = lotrCollectionMovies
        ),
        MovieCollectionSection(
            id = "nolan",
            title = "Cine de Autor: Colección Christopher Nolan",
            movies = nolanCollectionMovies
        )
    )

    val localCollectionsCatalog: List<MovieItem> = mcuCollectionMovies

    data class ScraperId(val tmdbId: String, val imdbId: String)
    data class MovieDefaultEmbed(
        val url: String,
        val serverName: String,
        val lang: String = "Latino",
        val quality: String = "Full HD"
    )

    private val movieResolverMap = mapOf(
        "avatar" to ScraperId("76600", "tt1630029"),
        "oppenheimer" to ScraperId("872585", "tt15398776"),
        "spider-man" to ScraperId("569094", "tt9362722"),
        "spider-verse" to ScraperId("569094", "tt9362722"),
        "interstellar" to ScraperId("157336", "tt0816692"),
        "interestelar" to ScraperId("157336", "tt0816692"),
        "dune" to ScraperId("693134", "tt15239678"),
        "duna" to ScraperId("693134", "tt15239678"),
        "batman" to ScraperId("155", "tt0468569"),
        "origen" to ScraperId("27205", "tt1375666"),
        "inception" to ScraperId("27205", "tt1375666"),
        "gladiador" to ScraperId("558449", "tt9603212"),
        "blade runner" to ScraperId("335984", "tt1856191"),
        "matrix" to ScraperId("603", "tt0133093")
    )

    // Pre-mapped verified LaMovie streaming embeds for curated catalog movies
    private val movieVerifiedLaMovieMap = mapOf(
        "avatar" to listOf(
            MovieDefaultEmbed("https://vimeos.net/embed-4pr30urll6bf.html", "Vimeos Turbo HD"),
            MovieDefaultEmbed("https://goodstream.one/embed-giea89mm0co3.html", "GoodStream HD")
        ),
        "oppenheimer" to listOf(
            MovieDefaultEmbed("https://vimeos.net/embed-klxgsdzm21sx.html", "Vimeos Turbo HD"),
            MovieDefaultEmbed("https://goodstream.one/embed-hjbk9go5ueiv.html", "GoodStream HD"),
            MovieDefaultEmbed("https://vimeos.net/embed-uhmqujnuu7x5.html", "Vimeos Alt HD")
        ),
        "interstellar" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-h5i4b3g7edng.html", "GoodStream HD")
        ),
        "interestelar" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-h5i4b3g7edng.html", "GoodStream HD")
        ),
        "dune" to listOf(
            MovieDefaultEmbed("https://vimeos.net/embed-kuyys5orh2c6.html", "Vimeos Turbo HD")
        ),
        "duna" to listOf(
            MovieDefaultEmbed("https://vimeos.net/embed-kuyys5orh2c6.html", "Vimeos Turbo HD")
        ),
        "gladiador" to listOf(
            MovieDefaultEmbed("https://vimeos.net/embed-k32v2qz0q9ug.html", "Vimeos Turbo HD")
        ),
        "blade runner" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-5kjm63nlpvcw.html", "GoodStream HD")
        ),
        "house-of-the-dragon" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-yjr8ypmzr648.html", "GoodStream HD"),
            MovieDefaultEmbed("https://vimeos.net/embed-7ny4vvdwblcm.html", "Vimeos Turbo HD")
        ),
        "casa del dragon" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-yjr8ypmzr648.html", "GoodStream HD"),
            MovieDefaultEmbed("https://vimeos.net/embed-7ny4vvdwblcm.html", "Vimeos Turbo HD")
        ),
        "casa del dragón" to listOf(
            MovieDefaultEmbed("https://goodstream.one/embed-yjr8ypmzr648.html", "GoodStream HD"),
            MovieDefaultEmbed("https://vimeos.net/embed-7ny4vvdwblcm.html", "Vimeos Turbo HD")
        )
    )

    private val titleAliases = mapOf(
        "la casa del dragon" to "house of the dragon",
        "la casa del dragón" to "house of the dragon",
        "casa del dragon" to "house of the dragon",
        "casa del dragón" to "house of the dragon",
        "house of the dragon" to "house of the dragon",
        "juego de tronos" to "game of thrones",
        "el caballero de la noche" to "the dark knight",
        "el origen" to "inception",
        "interestelar" to "interstellar",
        "ataque a los titanes" to "attack on titan",
        "guardianes de la noche" to "demon slayer",
        "la reina de las lagrimas" to "queen of tears",
        "la reina de las lágrimas" to "queen of tears"
    )

    private fun formatServerName(url: String, rawServer: String): String {
        val u = url.lowercase()
        return when {
            u.contains(".m3u8") || u.contains("master") -> "Stream Nativo HLS Ultra HD"
            u.contains(".mp4") -> "Stream Directo MP4 Full HD"
            u.contains("vimeos") -> "Vimeos Turbo HD"
            u.contains("goodstream") -> "GoodStream HD"
            u.contains("voe") || u.contains("megankid") || u.contains("cloudwindow") -> "VOE Stream HD"
            u.contains("dood") -> "DoodStream HD"
            rawServer.isNotBlank() && !rawServer.equals("online", ignoreCase = true) -> rawServer
            else -> "LaMovie HD"
        }
    }

    private fun languagePriority(sourceOrLang: String?): Int {
        val s = (sourceOrLang ?: "").lowercase()
        return when {
            s.contains("latino") || s.contains("lat") -> 0
            s.contains("dual") -> 1
            s.contains("castellano") || s.contains("español") || s.contains("espanol") -> 2
            s.contains("sub") || s.contains("ingl") || s.contains("engl") || s.contains("orig") -> 4
            else -> 3
        }
    }

    private fun serverPriority(url: String): Int {
        val u = url.lowercase()
        return when {
            u.contains(".m3u8") || u.contains("master") || u.contains("cloudwindow") -> 0
            u.contains(".mp4") -> 1
            u.contains("vimeos") -> 2
            u.contains("goodstream") -> 3
            u.contains("voe") || u.contains("megankid") -> 4
            u.contains("dood") -> 5
            else -> 6
        }
    }

    private fun isUnsupportedEmbedHost(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("hlswish") ||
            u.contains("filemoon") ||
            u.contains("videoapp.zip") ||
            u.contains("vidsrc") ||
            u.contains("multiembed") ||
            u.contains("smashystream") ||
            u.contains("lamovie.org/embed.html")
    }

    fun getStreamsForMovie(movieUrl: String): List<StreamItem> {
        val lower = movieUrl.lowercase()
        val verifiedEmbeds = movieVerifiedLaMovieMap.entries.firstOrNull { lower.contains(it.key) }?.value

        val streams = mutableListOf<StreamItem>()

        if (!verifiedEmbeds.isNullOrEmpty()) {
            verifiedEmbeds.forEachIndexed { idx, embed ->
                if (!isUnsupportedEmbedHost(embed.url)) {
                    streams.add(
                        StreamItem(
                            url = embed.url,
                            type = "EMBED",
                            source = "Servidor ${idx + 1}: ${embed.serverName} (${embed.lang} - ${embed.quality})",
                            foundVia = "LaMovie Engine"
                        )
                    )
                }
            }
        }

        return streams
    }

    private fun stripEpisodeSuffix(raw: String): String {
        return raw.replace(Regex("""\s*-\s*T\d+:E\d+.*""", RegexOption.IGNORE_CASE), "").trim()
    }

    private fun cleanTitleForMatch(raw: String): String {
        return stripEpisodeSuffix(raw)
            .replace(Regex("""\(\d{4}\)"""), "")
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun findBestMatchingPost(
        posts: org.json.JSONArray,
        targetTitle: String,
        expectedType: String? = null
    ): org.json.JSONObject? {
        if (posts.length() == 0) return null
        val targetClean = cleanTitleForMatch(targetTitle)
        if (targetClean.isEmpty()) return posts.getJSONObject(0)

        var bestPost: org.json.JSONObject? = null
        var bestScore = Int.MIN_VALUE

        for (i in 0 until posts.length()) {
            val p = posts.getJSONObject(i)
            val rawTitle = p.optString("title", "")
            val pType = p.optString("type", "")
            val candClean = cleanTitleForMatch(rawTitle)
            if (candClean.isEmpty()) continue

            if (targetClean.contains("avatar") && !targetClean.contains("aang") && candClean.contains("aang")) {
                continue
            }
            if (targetClean.contains("batman") && !targetClean.contains("ninja") && candClean.contains("ninja")) {
                continue
            }

            val targetAlias = titleAliases[targetClean]
            val aliasMatches = targetAlias != null && (candClean == targetAlias || candClean.startsWith("$targetAlias ") || candClean.contains(targetAlias))

            val lengthDiff = kotlin.math.abs(candClean.length - targetClean.length)
            var score = when {
                candClean == targetClean || (targetAlias != null && candClean == targetAlias) -> 1000
                aliasMatches -> 900
                candClean.startsWith("$targetClean ") -> 750 - (lengthDiff * 6)
                targetClean.startsWith("$candClean ") -> 720 - (lengthDiff * 6)
                candClean.contains(targetClean) -> 550 - (lengthDiff * 6)
                targetClean.contains(candClean) -> 500 - (lengthDiff * 6)
                else -> 100 - (lengthDiff * 4)
            }

            if (expectedType != null && pType == expectedType) {
                score += 350
            }

            if (!targetClean.contains("dilema") && !targetClean.contains("documental") &&
                (candClean.contains("dilema") || candClean.contains("documental"))
            ) {
                score -= 500
            }

            if (score > bestScore) {
                bestScore = score
                bestPost = p
            }
        }

        return bestPost ?: posts.getJSONObject(0)
    }

    suspend fun resolveLaMovieStreams(movieTitle: String): List<StreamItem> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamItem>()
        try {
            val cleanBase = stripEpisodeSuffix(movieTitle)
            val cleanKey = cleanTitleForMatch(cleanBase)
            val alias = titleAliases[cleanKey]
            val queryCandidates = mutableListOf(
                cleanBase.split("(")[0].trim(),
                cleanBase.split(":")[0].split("(")[0].trim()
            )
            if (alias != null) {
                queryCandidates.add(alias)
            }
            val distinctCandidates = queryCandidates.distinct()

            var targetPost: org.json.JSONObject? = null

            for (q in distinctCandidates) {
                val encoded = java.net.URLEncoder.encode(q, "UTF-8").replace("+", "%20")
                val searchUrl = "https://lamovie.org/wp-api/v1/search?q=$encoded&postType=any&postsPerPage=24"

                val searchReq = okhttp3.Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://lamovie.org/")
                    .build()

                val searchResp = ApiClient.okHttpClient.newCall(searchReq).execute()
                if (!searchResp.isSuccessful) continue

                val searchBody = searchResp.body?.string() ?: continue
                val searchJson = org.json.JSONObject(searchBody)
                val posts = searchJson.optJSONObject("data")?.optJSONArray("posts")
                if (posts != null && posts.length() > 0) {
                    targetPost = findBestMatchingPost(posts, movieTitle)
                    if (targetPost != null) break
                }
            }

            if (targetPost == null) return@withContext emptyList()

            val postId = targetPost.optInt("_id", -1)
            val postSlug = targetPost.optString("slug", "")
            val postType = targetPost.optString("type", "movies")
            if (postId <= 0) return@withContext emptyList()

            val actualTargetPostId = if (postType == "tvshows" || postType == "animes") {
                try {
                    val epUrl = "https://lamovie.org/wp-api/v1/single/episodes/list?_id=$postId&season=1&page=1&postsPerPage=5"
                    val epReq = okhttp3.Request.Builder()
                        .url(epUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://lamovie.org/")
                        .build()
                    val epResp = ApiClient.okHttpClient.newCall(epReq).execute()
                    if (epResp.isSuccessful) {
                        val epBody = epResp.body?.string() ?: ""
                        val epJson = org.json.JSONObject(epBody)
                        val epPosts = epJson.optJSONObject("data")?.optJSONArray("posts")
                        if (epPosts != null && epPosts.length() > 0) {
                            epPosts.getJSONObject(0).optInt("_id", postId)
                        } else postId
                    } else postId
                } catch (_: Exception) {
                    postId
                }
            } else {
                postId
            }

            val playerUrl = "https://lamovie.org/wp-api/v1/player?postId=$actualTargetPostId&demo=0"
            val playerReq = okhttp3.Request.Builder()
                .url(playerUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://lamovie.org/peliculas/$postSlug")
                .build()

            val playerResp = ApiClient.okHttpClient.newCall(playerReq).execute()
            if (!playerResp.isSuccessful) return@withContext emptyList()

            val playerBody = playerResp.body?.string() ?: return@withContext emptyList()
            val playerJson = org.json.JSONObject(playerBody)
            val embeds = playerJson.optJSONObject("data")?.optJSONArray("embeds")
            if (embeds != null) {
                for (i in 0 until embeds.length()) {
                    val em = embeds.getJSONObject(i)
                    val url = em.optString("url", "")
                    val server = em.optString("server", "LaMovie")
                    val lang = em.optString("lang", "Latino")
                    val quality = em.optString("quality", "Full HD")
                    if (url.isNotBlank() && !isUnsupportedEmbedHost(url)) {
                        if (url.contains("voe") || server.equals("voe", ignoreCase = true)) {
                            val voeStreams = extractVoeDirectStreams(url, lang, quality)
                            if (voeStreams.isNotEmpty()) {
                                streams.addAll(voeStreams)
                            } else {
                                val friendlyName = formatServerName(url, server)
                                streams.add(
                                    StreamItem(
                                        url = url,
                                        type = "EMBED",
                                        source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                        foundVia = "LaMovie Engine"
                                    )
                                )
                            }
                        } else if (url.contains("vimeos") || server.equals("vimeos", ignoreCase = true)) {
                            val vimeosStreams = extractVimeosDirectStreams(url, lang, quality)
                            if (vimeosStreams.isNotEmpty()) {
                                streams.addAll(vimeosStreams)
                            } else {
                                val friendlyName = formatServerName(url, server)
                                streams.add(
                                    StreamItem(
                                        url = url,
                                        type = "EMBED",
                                        source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                        foundVia = "LaMovie Engine"
                                    )
                                )
                            }
                        } else {
                            val friendlyName = formatServerName(url, server)
                            streams.add(
                                StreamItem(
                                    url = url,
                                    type = "EMBED",
                                    source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                    foundVia = "LaMovie Engine"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val allFoundSubtitles = streams.flatMap { it.safeSubtitles }.distinctBy { it.url }
        val enriched = if (allFoundSubtitles.isNotEmpty()) {
            streams.map { s ->
                val merged = (s.safeSubtitles + allFoundSubtitles).distinctBy { it.url }
                s.copy(
                    subtitle = 1,
                    subtitleTracks = merged
                )
            }
        } else {
            streams
        }
        filterWorkingStreams(enriched)
    }

    data class CatalogPageResult(
        val items: List<MovieItem> = emptyList(),
        val totalCount: Int = 0,
        val totalPages: Int = 1,
        val currentPage: Int = 1
    )

    data class TaxonomyOption(
        val id: Int,
        val name: String,
        val slug: String = ""
    )

    data class FilterState(
        val orderBy: String = "latest",
        val order: String = "desc",
        val selectedGenre: TaxonomyOption? = null,
        val selectedProvider: TaxonomyOption? = null,
        val selectedCountry: TaxonomyOption? = null,
        val selectedYear: TaxonomyOption? = null,
        val selectedTmdbMin: Double? = null
    ) {
        val hasActiveFilters: Boolean
            get() = selectedGenre != null || selectedProvider != null || selectedCountry != null ||
                    selectedYear != null || selectedTmdbMin != null || orderBy != "latest" || order != "desc"

        val activeFiltersCount: Int
            get() = (if (selectedGenre != null) 1 else 0) +
                    (if (selectedProvider != null) 1 else 0) +
                    (if (selectedCountry != null) 1 else 0) +
                    (if (selectedYear != null) 1 else 0) +
                    (if (selectedTmdbMin != null) 1 else 0) +
                    (if (orderBy != "latest" || order != "desc") 1 else 0)
    }

    val allTaxonomyGenres = listOf(
        TaxonomyOption(32, "Acción", "accion"),
        TaxonomyOption(705, "Acción & Aventura", "action-adventure"),
        TaxonomyOption(520, "Animación", "animacion"),
        TaxonomyOption(130, "Aventura", "aventura"),
        TaxonomyOption(3056, "Bélica", "belica"),
        TaxonomyOption(131, "Ciencia Ficción", "ciencia-ficcion"),
        TaxonomyOption(704, "Ciencia Ficción & Fantasía", "sci-fi-fantasy"),
        TaxonomyOption(18, "Comedia", "comedia"),
        TaxonomyOption(180, "Crimen", "crimen"),
        TaxonomyOption(164, "Documental", "documental"),
        TaxonomyOption(17, "Drama", "drama"),
        TaxonomyOption(398, "Familia", "familia"),
        TaxonomyOption(229, "Fantasía", "fantasia"),
        TaxonomyOption(786, "Guerra & Política", "war-politics"),
        TaxonomyOption(165, "Historia", "historia"),
        TaxonomyOption(703, "Kids", "kids"),
        TaxonomyOption(97, "Misterio", "misterio"),
        TaxonomyOption(8, "Música", "musica"),
        TaxonomyOption(6787, "Película de TV", "pelicula-de-tv"),
        TaxonomyOption(12485, "Reality", "reality"),
        TaxonomyOption(115, "Romance", "romance"),
        TaxonomyOption(19824, "Soap / Telenovelas", "soap"),
        TaxonomyOption(33, "Suspense", "suspense"),
        TaxonomyOption(96, "Terror", "terror"),
        TaxonomyOption(674, "Western", "western")
    )

    val movieGenres = allTaxonomyGenres
    val seriesGenres = allTaxonomyGenres
    val animeGenres = allTaxonomyGenres

    val providers = listOf(
        TaxonomyOption(572, "Netflix", "netflix"),
        TaxonomyOption(459, "Disney+", "disney-plus"),
        TaxonomyOption(675, "Max (HBO)", "max"),
        TaxonomyOption(563, "Amazon Prime Video", "amazon-prime-video"),
        TaxonomyOption(461, "Apple TV", "apple-tv"),
        TaxonomyOption(493, "Hulu", "hulu"),
        TaxonomyOption(549, "Claro Video", "claro-video"),
        TaxonomyOption(465, "Movistar+", "movistartv"),
        TaxonomyOption(460, "Google TV", "google-play-movies"),
        TaxonomyOption(470, "YouTube", "youtube")
    )

    val countries = listOf(
        TaxonomyOption(457, "Estados Unidos", "estados-unidos"),
        TaxonomyOption(2499, "España", "espana"),
        TaxonomyOption(5436, "México", "mexico"),
        TaxonomyOption(733, "Japón", "japon"),
        TaxonomyOption(4601, "Corea del Sur", "corea-del-sur"),
        TaxonomyOption(774, "Reino Unido", "reino-unido"),
        TaxonomyOption(617, "Francia", "francia"),
        TaxonomyOption(787, "Canadá", "canada"),
        TaxonomyOption(7746, "Argentina", "argentina"),
        TaxonomyOption(12155, "Colombia", "colombia"),
        TaxonomyOption(1431, "Alemania", "alemania"),
        TaxonomyOption(3912, "Italia", "italia")
    )

    val years = listOf(
        TaxonomyOption(74006, "2026", "2026"),
        TaxonomyOption(4, "2025", "2025"),
        TaxonomyOption(1354, "2024", "2024"),
        TaxonomyOption(2236, "2023", "2023"),
        TaxonomyOption(1461, "2022", "2022"),
        TaxonomyOption(2169, "2021", "2021"),
        TaxonomyOption(2792, "2020", "2020"),
        TaxonomyOption(1816, "2019", "2019"),
        TaxonomyOption(1926, "2018", "2018"),
        TaxonomyOption(8694, "2015", "2015"),
        TaxonomyOption(3858, "2010", "2010"),
        TaxonomyOption(684, "2000s", "2000")
    )

    val tmdbScoreOptions = listOf(
        8.0 to "8.0+ (Obras maestras)",
        7.0 to "7.0+ (Muy buenas)",
        6.0 to "6.0+ (Populares)"
    )

    val sortOptions = listOf(
        "latest" to "Más recientes",
        "popular" to "Más populares",
        "rated" to "Mejor valorados",
        "views" to "Más vistos"
    )

    private val categoryListingCache = java.util.concurrent.ConcurrentHashMap<String, CatalogPageResult>()

    suspend fun fetchCategoryListing(
        categoryType: String,
        page: Int = 1,
        postsPerPage: Int = 30,
        genreIds: List<Int> = emptyList(),
        providerIds: List<Int> = emptyList(),
        countryIds: List<Int> = emptyList(),
        yearIds: List<Int> = emptyList(),
        tmdbMin: Double? = null,
        tmdbMax: Double? = null,
        orderBy: String = "latest",
        order: String = "desc"
    ): CatalogPageResult = withContext(Dispatchers.IO) {
        val postType = when (categoryType) {
            "series", "tvshows" -> "tvshows"
            "animes", "anime" -> "animes"
            else -> "movies"
        }

        val cacheKey = "$postType-$page-$postsPerPage-${genreIds.sorted()}-$orderBy-$order-${tmdbMin ?: 0.0}-${tmdbMax ?: 0.0}"
        categoryListingCache[cacheKey]?.let { return@withContext it }

        try {
            val filterObj = JSONObject()
            if (genreIds.isNotEmpty()) {
                val gArr = JSONArray()
                genreIds.forEach { gArr.put(it) }
                filterObj.put("genres", gArr)
            }
            if (providerIds.isNotEmpty()) {
                val pArr = JSONArray()
                providerIds.forEach { pArr.put(it) }
                filterObj.put("providers", pArr)
            }
            if (countryIds.isNotEmpty()) {
                val cArr = JSONArray()
                countryIds.forEach { cArr.put(it) }
                filterObj.put("countries", cArr)
            }
            if (yearIds.isNotEmpty()) {
                val yArr = JSONArray()
                yearIds.forEach { yArr.put(it) }
                filterObj.put("years", yArr)
            }
            if (tmdbMin != null && tmdbMin > 0.0) {
                val tArr = JSONArray()
                tArr.put(tmdbMin)
                tArr.put(tmdbMax ?: 10.0)
                filterObj.put("tmdb", tArr)
            }

            val filterParam = if (filterObj.length() > 0) {
                "&filter=" + java.net.URLEncoder.encode(filterObj.toString(), "UTF-8")
            } else ""

            val url = "https://lamovie.org/wp-api/v1/listing/$postType?postType=$postType&page=$page&postsPerPage=$postsPerPage&orderBy=$orderBy&order=$order$filterParam"
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://lamovie.org/")
                .build()

            val resp = ApiClient.okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext CatalogPageResult(emptyList(), 0, 1, page)
            val body = resp.body?.string() ?: return@withContext CatalogPageResult(emptyList(), 0, 1, page)
            val json = org.json.JSONObject(body)
            val dataObj = json.optJSONObject("data") ?: return@withContext CatalogPageResult(emptyList(), 0, 1, page)

            val paginationObj = dataObj.optJSONObject("pagination")
            val totalCount = paginationObj?.optInt("total", 0) ?: 0
            val lastPage = paginationObj?.optInt("last_page", 1) ?: 1

            val posts = dataObj.optJSONArray("posts") ?: return@withContext CatalogPageResult(emptyList(), totalCount, lastPage, page)
            val items = mutableListOf<MovieItem>()

            for (i in 0 until posts.length()) {
                val p = posts.getJSONObject(i)
                val title = p.optString("title", "")
                val slug = p.optString("slug", "")
                val pType = p.optString("type", postType)
                val images = p.optJSONObject("images")
                val posterRel = images?.optString("poster", "") ?: ""
                val posterUrl = if (posterRel.isNotBlank()) {
                    if (posterRel.startsWith("http")) posterRel else "https://lamovie.org/wp-content/uploads$posterRel"
                } else ""

                val galleryFirst = p.optString("gallery", "")
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotBlank() }
                val backdropRel = images?.optString("backdrop", "") ?: ""
                val backdropUrl = when {
                    !galleryFirst.isNullOrBlank() ->
                        if (galleryFirst.startsWith("http")) galleryFirst else "https://image.tmdb.org/t/p/w1280$galleryFirst"
                    backdropRel.isNotBlank() ->
                        if (backdropRel.startsWith("http")) backdropRel else "https://lamovie.org/wp-content/uploads$backdropRel"
                    else -> null
                }

                val logoRel = images?.optString("logo", "") ?: ""
                val logoUrl = if (logoRel.isNotBlank()) {
                    if (logoRel.startsWith("http")) logoRel else "https://lamovie.org/wp-content/uploads$logoRel"
                } else null

                val overview = p.optString("overview", "").ifBlank { null }
                val runtimeStr = p.optString("runtime", "").ifBlank { null }

                val ratingVal = p.optString("rating", "7.5").ifBlank { "7.5" }
                val releaseDate = p.optString("release_date", "")
                val yearMatch = Regex("""\((\d{4})\)""").find(title)?.groupValues?.get(1)
                    ?: if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2025"

                val itemPath = when (pType) {
                    "tvshows" -> "series"
                    "animes" -> "animes"
                    else -> "peliculas"
                }

                val parsedGenres = mutableListOf<String>()
                val gArray = p.optJSONArray("genres")
                if (gArray != null) {
                    for (gi in 0 until gArray.length()) {
                        val gId = gArray.optInt(gi, -1)
                        val gName = laMovieGenresMap[gId]
                        if (gName != null && !parsedGenres.contains(gName)) {
                            parsedGenres.add(gName)
                        }
                    }
                }

                if (title.isNotBlank() && posterUrl.isNotBlank()) {
                    items.add(
                        MovieItem(
                            title = title,
                            url = "https://lamovie.org/$itemPath/$slug",
                            image = posterUrl,
                            backdrop = backdropUrl,
                            logo = logoUrl,
                            type = if (pType == "tvshows") "series" else if (pType == "animes") "anime" else "normal",
                            rating = ratingVal,
                            year = yearMatch,
                            quality = "Full HD",
                            languages = listOf("Latino"),
                            overview = overview,
                            runtime = runtimeStr,
                            genres = parsedGenres
                        )
                    )
                }
            }
            val result = CatalogPageResult(items, totalCount, lastPage, page)
            if (items.isNotEmpty()) {
                categoryListingCache[cacheKey] = result
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            CatalogPageResult(emptyList(), 0, 1, page)
        }
    }

    suspend fun fetchLaMovieListing(page: Int = 1): List<MovieItem> = withContext(Dispatchers.IO) {
        fetchCategoryListing("movies", page, postsPerPage = 30).items
    }

    suspend fun getCatalog(page: Int = 1): MovieListResponse = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(6500L) {
            fetchCategoryListing("movies", page, postsPerPage = 30)
        } ?: CatalogPageResult()

        val liveMovies = result.items
        if (liveMovies.isEmpty()) {
            return@withContext MovieListResponse(
                status = "error",
                currentPage = page,
                totalPages = 0,
                totalCount = 0,
                hasNextPage = false,
                featured = emptyList(),
                movies = emptyList()
            )
        }

        // Live featured movies for the top hero banner:
        // Prioritize those with high-resolution backdrops, select up to 6 prominent titles
        val featured = liveMovies
            .filter { !it.backdrop.isNullOrBlank() }
            .take(6)
            .ifEmpty { liveMovies.take(5) }

        MovieListResponse(
            status = "success",
            currentPage = page,
            totalPages = result.totalPages.coerceAtLeast(1),
            totalCount = if (result.totalCount > 0) result.totalCount else liveMovies.size,
            hasNextPage = page < result.totalPages,
            featured = featured,
            movies = liveMovies
        )
    }

    suspend fun isServerAlive(stream: StreamItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isUnsupportedEmbedHost(stream.url)) return@withContext false

            val isDirectStream = stream.type == "HLS" || stream.type == "MP4" ||
                stream.url.contains(".m3u8") || stream.url.contains(".mp4")

            val streamReferer = if (stream.url.contains("vimeos")) "https://vimeos.net/" else "https://lamovie.org/"
            val streamOrigin = if (stream.url.contains("vimeos")) "https://vimeos.net" else "https://lamovie.org"

            val reqBuilder = okhttp3.Request.Builder()
                .url(stream.url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", streamReferer)
                .header("Origin", streamOrigin)

            if (isDirectStream) {
                if (stream.foundVia?.contains("Vimeos Native") == true || stream.foundVia?.contains("VOE Native") == true) {
                    return@withContext true
                }
                reqBuilder.head()
                val resp = ApiClient.healthCheckOkHttpClient.newCall(reqBuilder.build()).execute()
                return@withContext resp.isSuccessful
            }

            var resp = ApiClient.healthCheckOkHttpClient.newCall(reqBuilder.build()).execute()
            if (!resp.isSuccessful) return@withContext false

            var html = resp.body?.string()?.lowercase() ?: return@withContext false

            // Follow JS redirect if present (e.g. voe.sx -> megankiddifference.com)
            val redirectMatch = Regex("""window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""").find(html)
            if (redirectMatch != null) {
                val nextUrl = redirectMatch.groupValues[1]
                val req2 = okhttp3.Request.Builder()
                    .url(nextUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://lamovie.org/")
                    .build()
                val resp2 = ApiClient.healthCheckOkHttpClient.newCall(req2).execute()
                if (resp2.isSuccessful) {
                    html = resp2.body?.string()?.lowercase() ?: html
                }
            }

            val deadKeywords = listOf(
                "no longer available",
                "expired or has been deleted",
                "file was deleted",
                "file has been deleted",
                "file not found",
                "video has been removed",
                "video is deleted",
                "video not found",
                "contenido no disponible",
                "byse frontend",
                "404 not found",
                "232001",
                "no se puede reproducir",
                "archivo de video no se puede reproducir",
                "video file cannot be played"
            )
            if (deadKeywords.any { html.contains(it) }) {
                return@withContext false
            }

            val hasValidPlayer = html.length > 2000 && (
                html.contains("jwplayer") ||
                html.contains("application/json") ||
                html.contains("eval(function(p,a,c,k,e,d)") ||
                html.contains("sources:") ||
                html.contains("sources :") ||
                html.contains(".m3u8") ||
                html.contains(".mp4") ||
                html.contains("<video") ||
                html.contains("cloudwindow")
            )
            return@withContext hasValidPlayer
        } catch (_: Exception) {
            false
        }
    }

    suspend fun filterWorkingStreams(streams: List<StreamItem>): List<StreamItem> = withContext(Dispatchers.IO) {
        val candidateList = streams
            .filter { it.url.isNotBlank() && !isUnsupportedEmbedHost(it.url) }
            .distinctBy { it.url }
            .sortedWith(
                compareBy<StreamItem> { languagePriority(it.source) }
                    .thenByDescending { it.safeSubtitles.isNotEmpty() }
                    .thenBy { serverPriority(it.url) }
            )

        if (candidateList.isEmpty()) return@withContext emptyList()
        try {
            supervisorScope {
                val checkDeferreds = candidateList.map { stream ->
                    async {
                        val alive = withTimeoutOrNull(3500L) { isServerAlive(stream) } ?: false
                        if (alive) stream else null
                    }
                }
                val verified = checkDeferreds.awaitAll().filterNotNull()
                val finalList = if (verified.isNotEmpty()) verified else candidateList
                finalList.mapIndexed { idx, s ->
                    val cleanSource = s.source?.replace(Regex("^Servidor \\d+:\\s*"), "")
                        ?: "${formatServerName(s.url, "")} (Latino - Full HD)"
                    s.copy(source = "Servidor ${idx + 1}: $cleanSource")
                }
            }
        } catch (_: Exception) {
            candidateList
        }
    }

    private val episodesCache = java.util.concurrent.ConcurrentHashMap<String, List<EpisodeItem>>()
    val seasonsCountCache = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    fun getSeasonsCount(postId: Int): Int = seasonsCountCache[postId] ?: 1

    suspend fun fetchEpisodesForSeason(
        postId: Int,
        season: Int,
        fallbackTitle: String,
        fallbackBackdrop: String,
        fallbackGallery: List<String> = emptyList()
    ): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val cacheKey = "${postId}_${season}"
        val cached = episodesCache[cacheKey]
        if (cached != null && cached.isNotEmpty()) {
            return@withContext cached
        }

        val episodes = mutableListOf<EpisodeItem>()
        if (postId > 0) {
            try {
                var currentPage = 1
                var maxPages = 1
                while (currentPage <= maxPages && currentPage <= 10) {
                    val epUrl = "https://lamovie.org/wp-api/v1/single/episodes/list?_id=$postId&season=$season&page=$currentPage&postsPerPage=250"
                    val epReq = okhttp3.Request.Builder()
                        .url(epUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://lamovie.org/")
                        .build()
                    val epResp = ApiClient.okHttpClient.newCall(epReq).execute()
                    if (epResp.isSuccessful) {
                        val epBody = epResp.body?.string() ?: ""
                        val epJson = org.json.JSONObject(epBody)
                        val dataObj = epJson.optJSONObject("data")
                        val epPosts = dataObj?.optJSONArray("posts")
                        val pagination = dataObj?.optJSONObject("pagination")

                        // Discover and remember full list of seasons reported by LaMovie
                        val seasonsArr = dataObj?.optJSONArray("seasons")
                        if (seasonsArr != null && seasonsArr.length() > 0) {
                            var maxS = 0
                            for (si in 0 until seasonsArr.length()) {
                                val sRaw = seasonsArr.opt(si)
                                val sNum = when (sRaw) {
                                    is Number -> sRaw.toInt()
                                    is String -> sRaw.trim().toIntOrNull() ?: 0
                                    else -> 0
                                }
                                if (sNum > maxS) maxS = sNum
                            }
                            if (maxS <= 0) maxS = seasonsArr.length()
                            if (maxS > 0) {
                                val prev = seasonsCountCache[postId] ?: 0
                                if (maxS > prev) {
                                    seasonsCountCache[postId] = maxS
                                    for ((key, cachedDetail) in movieDetailCache) {
                                        if (cachedDetail.postId == postId.toString() && cachedDetail.seasonsCount < maxS) {
                                            movieDetailCache[key] = cachedDetail.copy(seasonsCount = maxS)
                                        }
                                    }
                                }
                            }
                        }
                        if (pagination != null) {
                            maxPages = pagination.optInt("last_page", 1)
                        }
                        if (epPosts != null && epPosts.length() > 0) {
                            for (i in 0 until epPosts.length()) {
                                val ep = epPosts.getJSONObject(i)
                                val epId = ep.optInt("_id", 0)
                                val epNum = ep.optInt("episode_number", ep.optInt("episode", episodes.size + 1))
                                val sNum = ep.optInt("season_number", ep.optInt("season", season))
                                val rawTitle = ep.optString("title", "").trim()
                                val cleanEpTitle = rawTitle.ifBlank { "Episodio $epNum" }
                                val epOverview = ep.optString("overview", "").trim()
                                    .ifBlank { "Episodio $epNum de la temporada $sNum de $fallbackTitle en alta definición con audio latino." }
                                val epRuntime = ep.optString("runtime", "").trim().ifBlank { "24m" }
                                val epSlug = ep.optString("slug", "")

                                val images = ep.optJSONObject("images")
                                val rawStill = (images?.optString("backdrop", "")?.ifBlank { images.optString("poster", "") }
                                    ?: ep.optString("still_path", "")).trim()
                                val stillRel = if (rawStill.equals("null", ignoreCase = true)) "" else rawStill

                                val sceneFallback = if (fallbackGallery.isNotEmpty()) {
                                    fallbackGallery[episodes.size % fallbackGallery.size]
                                } else {
                                    fallbackBackdrop
                                }

                                val epImage = when {
                                    stillRel.startsWith("http") -> stillRel
                                    stillRel.startsWith("/backdrops/") ||
                                        stillRel.startsWith("/thumbs/") ||
                                        stillRel.startsWith("/posters/") ||
                                        stillRel.startsWith("/logos/") -> "https://lamovie.org/wp-content/uploads$stillRel"
                                    stillRel.startsWith("/") -> "https://image.tmdb.org/t/p/w500$stillRel"
                                    else -> sceneFallback
                                }

                                episodes.add(
                                    EpisodeItem(
                                        id = epId,
                                        episodeNumber = epNum,
                                        seasonNumber = sNum,
                                        title = cleanEpTitle,
                                        overview = epOverview,
                                        image = epImage,
                                        runtime = if (epRuntime.endsWith("m") || epRuntime.contains("h")) epRuntime else "${epRuntime}m",
                                        slug = epSlug
                                    )
                                )
                            }
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                    currentPage++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (episodes.isEmpty()) {
            val defaultNames = listOf(
                "El Comienzo",
                "Alianzas Ocultas",
                "Punto de Quiebre",
                "Ecos del Pasado",
                "La Sombra",
                "El Despertar",
                "Sin Retorno",
                "El Desenlace"
            )
            for (idx in defaultNames.indices) {
                val epNum = idx + 1
                val sceneFallback = if (fallbackGallery.isNotEmpty()) {
                    fallbackGallery[idx % fallbackGallery.size]
                } else {
                    fallbackBackdrop
                }
                episodes.add(
                    EpisodeItem(
                        id = -(season * 100 + epNum),
                        episodeNumber = epNum,
                        seasonNumber = season,
                        title = "Episodio $epNum: ${defaultNames[idx]}",
                        overview = "Capítulo $epNum (Temporada $season) de $fallbackTitle disponible en 1080p Full HD con audio español latino.",
                        image = sceneFallback,
                        runtime = "48m"
                    )
                )
            }
        } else {
            episodesCache[cacheKey] = episodes
        }
        episodes
    }

    private fun rot13(str: String): String {
        val sb = StringBuilder(str.length)
        for (c in str) {
            when (c) {
                in 'a'..'z' -> sb.append(((c - 'a' + 13) % 26 + 'a'.code).toChar())
                in 'A'..'Z' -> sb.append(((c - 'A' + 13) % 26 + 'A'.code).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun decodeVoeConfig(encoded: String): JSONObject? {
        return try {
            var decoded = rot13(encoded)
            val markers = listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&")
            for (m in markers) {
                decoded = decoded.replace(m, "")
            }
            val firstBytes = Base64.decode(decoded, Base64.DEFAULT)
            val shifted = ByteArray(firstBytes.size)
            for (i in firstBytes.indices) {
                val b = firstBytes[i].toInt() and 0xFF
                shifted[i] = (b - 3).toByte()
            }
            val reversed = shifted.reversedArray()
            val reversedBase64 = String(reversed, Charsets.UTF_8)
            val jsonBytes = Base64.decode(reversedBase64, Base64.DEFAULT)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            JSONObject(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractVoeDirectStreams(
        embedUrl: String,
        lang: String = "Latino",
        quality: String = "Full HD"
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<StreamItem>()
        try {
            var targetUrl = embedUrl
            val req1 = okhttp3.Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://lamovie.org/")
                .build()
            val resp1 = ApiClient.streamingOkHttpClient.newCall(req1).execute()
            if (!resp1.isSuccessful) return@withContext emptyList()
            var body = resp1.body?.string() ?: ""

            // Check for JS redirect (e.g. voe.sx -> megankiddifference.com)
            val redirectMatch = Regex("""window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""").find(body)
            if (redirectMatch != null) {
                targetUrl = redirectMatch.groupValues[1]
                val req2 = okhttp3.Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://lamovie.org/")
                    .build()
                val resp2 = ApiClient.streamingOkHttpClient.newCall(req2).execute()
                if (resp2.isSuccessful) {
                    body = resp2.body?.string() ?: ""
                }
            }

            val displayQuality = if (quality.isNotBlank()) quality else "1080p Ultra HD"
            val displayLang = if (lang.isNotBlank()) lang else "Latino - Inglés"

            // Look for <script type="application/json"> with encoded config
            val scriptPattern = Regex("""<script\b[^>]*type=["']application/json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val jsonMatches = scriptPattern.findAll(body)
            for (match in jsonMatches) {
                val scriptContent = match.groupValues[1].trim()
                try {
                    val arr = JSONArray(scriptContent)
                    for (i in 0 until arr.length()) {
                        val encoded = arr.optString(i, "")
                        if (encoded.length > 20) {
                            val config = decodeVoeConfig(encoded)
                            if (config != null) {
                                val hlsSource = config.optString("source", "")
                                val mp4Source = config.optString("direct_access_url", "")

                                if (hlsSource.isNotBlank()) {
                                    results.add(
                                        StreamItem(
                                            url = hlsSource,
                                            type = "HLS",
                                            source = "Servidor ${results.size + 1}: VOE Nativo Ultra HD ($displayLang - $displayQuality)",
                                            foundVia = "VOE Native HLS"
                                        )
                                    )
                                }
                                if (mp4Source.isNotBlank() && results.none { it.url == mp4Source }) {
                                    results.add(
                                        StreamItem(
                                            url = mp4Source,
                                            type = "MP4",
                                            source = "Servidor ${results.size + 1}: VOE Directo MP4 ($displayLang)",
                                            foundVia = "VOE Direct MP4"
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Fallback: check sources = { 'hls': 'base64...' }
            if (results.isEmpty()) {
                val base64HlsMatch = Regex("""sources\s*=\s*\{.*?['"]hls['"]\s*:\s*['"]([^'"]+)['"]""", RegexOption.DOT_MATCHES_ALL).find(body)
                if (base64HlsMatch != null) {
                    val b64 = base64HlsMatch.groupValues[1]
                    try {
                        val decodedBytes = Base64.decode(b64, Base64.DEFAULT)
                        val hlsUrl = String(decodedBytes, Charsets.UTF_8)
                        if (hlsUrl.startsWith("http")) {
                            results.add(
                                StreamItem(
                                    url = hlsUrl,
                                    type = "HLS",
                                    source = "Servidor 1: VOE Nativo HD ($displayLang - $displayQuality)",
                                    foundVia = "VOE Native HLS"
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            // Always add the resolved embed URL as secondary server
            results.add(
                StreamItem(
                    url = targetUrl,
                    type = "EMBED",
                    source = "Servidor ${results.size + 1}: VOE Web Player ($displayLang - $displayQuality)",
                    foundVia = "VOE Web Embed"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    private fun encodeBase(c: Int, a: Int): String {
        val prefix = if (c < a) "" else encodeBase(c / a, a)
        val remainder = c % a
        val ch = if (remainder > 35) {
            (remainder + 29).toChar()
        } else {
            java.lang.Integer.toString(remainder, 36)[0]
        }
        return prefix + ch
    }

    fun unpackPackedJs(htmlOrJs: String): String? {
        return try {
            val pRegex = Regex("""eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*d\s*\)[\s\S]*?\}\s*\(\s*['"]([\s\S]*?)['"]\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*['"]([\s\S]*?)['"]\.split\('\|'\)""")
            val match = pRegex.find(htmlOrJs) ?: return null
            val (p, aStr, cStr, kStr) = match.destructured
            val a = aStr.toIntOrNull() ?: return null
            val c = cStr.toIntOrNull() ?: return null
            val k = kStr.split('|')

            val dict = mutableMapOf<String, String>()
            for (i in (c - 1) downTo 0) {
                val key = encodeBase(i, a)
                val value = k.getOrNull(i)
                if (!value.isNullOrEmpty()) {
                    dict[key] = value
                }
            }

            val wordRegex = Regex("""\b\w+\b""")
            wordRegex.replace(p) { m ->
                dict[m.value] ?: m.value
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun extractVimeosDirectStreams(
        embedUrl: String,
        lang: String = "Latino",
        quality: String = "Full HD"
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<StreamItem>()
        try {
            val fileCode = embedUrl.substringAfterLast("/").removeSuffix(".html").substringAfterLast("-").trim()
            if (fileCode.isBlank()) return@withContext emptyList()

            val displayQuality = if (quality.isNotBlank()) quality else "1080p Full HD"
            val displayLang = if (lang.isNotBlank()) lang else "Latino"

            // 1. Submit POST /dl request mimicking the native player handshake
            val formBody = okhttp3.FormBody.Builder()
                .add("op", "embed")
                .add("file_code", fileCode)
                .add("auto", "1")
                .add("referer", "https://lamovie.org/")
                .build()

            val req = okhttp3.Request.Builder()
                .url("https://vimeos.net/dl")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", embedUrl)
                .header("Origin", "https://vimeos.net")
                .build()

            val resp = ApiClient.streamingOkHttpClient.newCall(req).execute()
            var body = if (resp.isSuccessful) resp.body?.string() ?: "" else ""

            // Fallback: If POST didn't return content, attempt GET on embedUrl directly
            if (body.isBlank() || body.length < 200) {
                val getReq = okhttp3.Request.Builder()
                    .url(embedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://lamovie.org/")
                    .build()
                val getResp = ApiClient.streamingOkHttpClient.newCall(getReq).execute()
                if (getResp.isSuccessful) {
                    body = getResp.body?.string() ?: ""
                }
            }

            // Check if file is reported dead/expired by Vimeos
            val deadKeywords = listOf(
                "no longer available",
                "expired or has been deleted",
                "file was deleted",
                "file has been deleted",
                "file not found"
            )
            if (deadKeywords.any { body.contains(it, ignoreCase = true) }) {
                return@withContext emptyList()
            }

            // Unpack Dean Edwards JS or search body for direct master.m3u8
            val unpacked = unpackPackedJs(body) ?: body
            val m3u8Regex = Regex("""(?:file|source|src)\s*:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE)
            var m3u8Match = m3u8Regex.find(unpacked)?.groupValues?.get(1)
            if (m3u8Match == null) {
                val genericM3u8Regex = Regex("""https?://[^"'\s<>]+\.m3u8[^"'\s<>]*""")
                m3u8Match = genericM3u8Regex.find(unpacked)?.value
            }

            val extractedSubtitles = extractSubtitleTracksFromUnpacked(unpacked, "https://vimeos.net")
            val subFlag = if (extractedSubtitles.isNotEmpty()) 1 else 0

            if (!m3u8Match.isNullOrBlank()) {
                results.add(
                    StreamItem(
                        url = m3u8Match,
                        type = "HLS",
                        source = "Servidor 1: Vimeos Nativo HD ($displayLang - $displayQuality)",
                        foundVia = "Vimeos Native HLS",
                        subtitle = subFlag,
                        subtitleTracks = extractedSubtitles
                    )
                )
            }

            // Always add the web embed as secondary/fallback server
            results.add(
                StreamItem(
                    url = embedUrl,
                    type = "EMBED",
                    source = "Servidor ${results.size + 1}: Vimeos Web Player ($displayLang - $displayQuality)",
                    foundVia = "Vimeos Web Embed",
                    subtitle = subFlag,
                    subtitleTracks = extractedSubtitles
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    fun extractSubtitleTracksFromUnpacked(unpackedHtmlOrJs: String, baseHost: String = "https://vimeos.net"): List<SubtitleTrackData> {
        val subtitles = mutableListOf<SubtitleTrackData>()
        try {
            val tracksBlockRegex = Regex("""tracks\s*:\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE)
            val match = tracksBlockRegex.find(unpackedHtmlOrJs)
            if (match != null) {
                val content = match.groupValues[1]
                val objRegex = Regex("""\{([^{}]+)\}""")
                for (m in objRegex.findAll(content)) {
                    val itemStr = m.groupValues[1]
                    val fileRegex = Regex("""(?:file|src)\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    val labelRegex = Regex("""label\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    val defaultRegex = Regex("""["']?default["']?\s*:\s*(true|1)""", RegexOption.IGNORE_CASE)

                    val file = fileRegex.find(itemStr)?.groupValues?.get(1)?.trim() ?: continue
                    val label = labelRegex.find(itemStr)?.groupValues?.get(1)?.trim() ?: "Subtítulo"
                    val isDefault = defaultRegex.containsMatchIn(itemStr)

                    if (label.contains("upload", ignoreCase = true) || file.contains("upload", ignoreCase = true)) {
                        continue
                    }
                    if (!file.contains(".vtt", ignoreCase = true) && !file.contains(".srt", ignoreCase = true)) {
                        continue
                    }
                    if (file.contains("vimeos", ignoreCase = true) || baseHost.contains("vimeos", ignoreCase = true)) {
                        continue
                    }

                    val fullUrl = when {
                        file.startsWith("http://") || file.startsWith("https://") -> file
                        file.startsWith("//") -> "https:$file"
                        file.startsWith("/") -> "$baseHost$file"
                        else -> "$baseHost/$file"
                    }

                    val lang = when {
                        label.contains("span", ignoreCase = true) || label.contains("españ", ignoreCase = true) || fullUrl.contains("_spa", ignoreCase = true) || fullUrl.contains("_es", ignoreCase = true) -> "es"
                        label.contains("eng", ignoreCase = true) || label.contains("ingl", ignoreCase = true) || fullUrl.contains("_eng", ignoreCase = true) || fullUrl.contains("_en", ignoreCase = true) -> "en"
                        label.contains("port", ignoreCase = true) || fullUrl.contains("_por", ignoreCase = true) || fullUrl.contains("_pt", ignoreCase = true) -> "pt"
                        else -> "es"
                    }

                    val cleanLabel = when (lang) {
                        "es" -> if (label.isNotBlank() && !label.equals("spanish", true) && !label.equals("español", true)) label else "Español"
                        "en" -> if (label.isNotBlank() && !label.equals("english", true) && !label.equals("inglés", true)) label else "Inglés"
                        "pt" -> if (label.isNotBlank() && !label.equals("portuguese", true) && !label.equals("portugues", true) && !label.equals("português", true)) label else "Portugués"
                        else -> label
                    }

                    if (subtitles.none { it.url == fullUrl }) {
                        subtitles.add(
                            SubtitleTrackData(
                                url = fullUrl,
                                label = cleanLabel,
                                language = lang,
                                isDefault = isDefault
                            )
                        )
                    }
                }
            }

            // Fallback: search for standalone .vtt URLs if tracks block was not parsed
            if (subtitles.isEmpty()) {
                val vttUrlRegex = Regex("""https?://[^"'\s<>]+\.vtt[^"'\s<>]*""", RegexOption.IGNORE_CASE)
                for (vttMatch in vttUrlRegex.findAll(unpackedHtmlOrJs)) {
                    val vttUrl = vttMatch.value
                    if (!vttUrl.contains("upload", ignoreCase = true) && subtitles.none { it.url == vttUrl }) {
                        val isSpanish = vttUrl.contains("spa", true) || vttUrl.contains("_es", true)
                        val isEnglish = vttUrl.contains("eng", true) || vttUrl.contains("_en", true)
                        val lang = if (isEnglish) "en" else "es"
                        val label = if (isEnglish) "Inglés" else "Español"
                        subtitles.add(
                            SubtitleTrackData(
                                url = vttUrl,
                                label = label,
                                language = lang,
                                isDefault = isSpanish
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return subtitles
    }

    suspend fun resolveEpisodeStreams(
        episodeId: Int,
        seriesTitle: String,
        fallbackStreams: List<StreamItem>,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        seriesPostId: Int? = null
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        var resolvedEpId = episodeId
        if (resolvedEpId <= 0) {
            try {
                var sPostId = seriesPostId ?: -1
                if (sPostId <= 0 && seriesTitle.isNotBlank()) {
                    val qRaw = stripEpisodeSuffix(seriesTitle).split("(")[0].trim()
                    val encoded = java.net.URLEncoder.encode(qRaw, "UTF-8").replace("+", "%20")
                    val searchUrl = "https://lamovie.org/wp-api/v1/search?q=$encoded&postType=tvshows&postsPerPage=10"
                    val sReq = okhttp3.Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://lamovie.org/")
                        .build()
                    val sResp = ApiClient.okHttpClient.newCall(sReq).execute()
                    if (sResp.isSuccessful) {
                        val sBody = sResp.body?.string() ?: ""
                        val sJson = JSONObject(sBody)
                        val sPosts = sJson.optJSONObject("data")?.optJSONArray("posts")
                        if (sPosts != null && sPosts.length() > 0) {
                            val p = findBestMatchingPost(sPosts, qRaw, expectedType = "tvshows") ?: sPosts.optJSONObject(0)
                            if (p != null) {
                                sPostId = p.optInt("_id", -1)
                            }
                        }
                    }
                }

                if (sPostId > 0) {
                    val seasonEps = fetchEpisodesForSeason(
                        postId = sPostId,
                        season = seasonNumber,
                        fallbackTitle = seriesTitle,
                        fallbackBackdrop = ""
                    )
                    val matchedEp = seasonEps.firstOrNull { it.episodeNumber == episodeNumber }
                        ?: seasonEps.getOrNull(episodeNumber - 1)
                    if (matchedEp != null && matchedEp.id > 0) {
                        resolvedEpId = matchedEp.id
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (resolvedEpId <= 0) return@withContext filterWorkingStreams(fallbackStreams)
        val streams = mutableListOf<StreamItem>()
        try {
            val playerUrl = "https://lamovie.org/wp-api/v1/player?postId=$resolvedEpId&demo=0"
            val playerReq = okhttp3.Request.Builder()
                .url(playerUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://lamovie.org/")
                .build()
            val playerResp = ApiClient.okHttpClient.newCall(playerReq).execute()
            if (playerResp.isSuccessful) {
                val playerBody = playerResp.body?.string() ?: ""
                val playerJson = JSONObject(playerBody)
                val embeds = playerJson.optJSONObject("data")?.optJSONArray("embeds")
                if (embeds != null) {
                    for (i in 0 until embeds.length()) {
                        val em = embeds.getJSONObject(i)
                        val url = em.optString("url", "")
                        val server = em.optString("server", "LaMovie")
                        val lang = em.optString("lang", "Latino")
                        val quality = em.optString("quality", "Full HD")
                        if (url.isNotBlank() && !isUnsupportedEmbedHost(url)) {
                            if (url.contains("voe") || server.equals("voe", ignoreCase = true)) {
                                val voeStreams = extractVoeDirectStreams(url, lang, quality)
                                if (voeStreams.isNotEmpty()) {
                                    streams.addAll(voeStreams)
                                } else {
                                    val friendlyName = formatServerName(url, server)
                                    streams.add(
                                        StreamItem(
                                            url = url,
                                            type = "EMBED",
                                            source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                            foundVia = "LaMovie Engine"
                                        )
                                    )
                                }
                            } else if (url.contains("vimeos") || server.equals("vimeos", ignoreCase = true)) {
                                val vimeosStreams = extractVimeosDirectStreams(url, lang, quality)
                                if (vimeosStreams.isNotEmpty()) {
                                    streams.addAll(vimeosStreams)
                                } else {
                                    val friendlyName = formatServerName(url, server)
                                    streams.add(
                                        StreamItem(
                                            url = url,
                                            type = "EMBED",
                                            source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                            foundVia = "LaMovie Engine"
                                        )
                                    )
                                }
                            } else {
                                val friendlyName = formatServerName(url, server)
                                streams.add(
                                    StreamItem(
                                        url = url,
                                        type = "EMBED",
                                        source = "Servidor ${streams.size + 1}: $friendlyName ($lang - $quality)",
                                        foundVia = "LaMovie Engine"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val allFoundSubtitles = streams.flatMap { it.safeSubtitles }.distinctBy { it.url }
        val enriched = if (allFoundSubtitles.isNotEmpty()) {
            streams.map { s ->
                val merged = (s.safeSubtitles + allFoundSubtitles).distinctBy { it.url }
                s.copy(
                    subtitle = 1,
                    subtitleTracks = merged
                )
            }
        } else {
            streams
        }
        val verified = filterWorkingStreams(enriched)
        if (verified.isNotEmpty()) verified else filterWorkingStreams(fallbackStreams)
    }

    private val movieDetailCache = java.util.concurrent.ConcurrentHashMap<String, MovieDetailResponse>()
    private val titleSlugCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, String>>()

    fun getCachedDetail(url: String, title: String? = null): MovieDetailResponse? {
        val cleanUrl = url.trim().trimEnd('/')
        val byUrl = movieDetailCache[cleanUrl]
        if (byUrl != null) return byUrl
        if (!title.isNullOrBlank()) {
            val cleanTitle = stripEpisodeSuffix(title).trim()
            val byTitle = movieDetailCache[cleanTitle]
            if (byTitle != null) return byTitle
        }
        return null
    }

    fun cacheDetail(url: String, title: String?, response: MovieDetailResponse) {
        val cleanUrl = url.trim().trimEnd('/')
        movieDetailCache[cleanUrl] = response
        if (!title.isNullOrBlank()) {
            val cleanTitle = stripEpisodeSuffix(title).trim()
            movieDetailCache[cleanTitle] = response
        }
        response.title?.let {
            val cleanRespTitle = stripEpisodeSuffix(it).trim()
            movieDetailCache[cleanRespTitle] = response
        }
    }

    suspend fun preloadMovie(movie: MovieItem) = withContext(Dispatchers.IO) {
        try {
            if (getCachedDetail(movie.url, movie.title) == null) {
                extractMovie(movie)
            }
        } catch (_: Exception) {}
    }

    suspend fun extractMovie(movie: MovieItem, forceRefresh: Boolean = false): MovieDetailResponse = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = getCachedDetail(movie.url, movie.title)
            if (cached != null) {
                if (cached.isSeries) {
                    val pId = cached.postId?.toIntOrNull() ?: movie.postId ?: -1
                    val detected = if (pId > 0) seasonsCountCache[pId] ?: 0 else 0
                    if (detected > cached.seasonsCount) {
                        val updated = cached.copy(seasonsCount = detected)
                        cacheDetail(movie.url, movie.title, updated)
                        return@withContext updated
                    }
                    if (cached.seasonsCount <= 1) {
                        // Stale series with 1 season: let it re-extract below to discover full seasons
                    } else {
                        return@withContext cached
                    }
                } else {
                    return@withContext cached
                }
            }
        }

        val cleanUrl = movie.url.trim().trimEnd('/')
        var slug = movie.slug?.ifBlank { null } ?: if (cleanUrl.contains("lamovie.org")) {
            cleanUrl.substringAfterLast("/").trim()
        } else null

        var postType = when {
            cleanUrl.contains("/tvshows/") || cleanUrl.contains("/series/") || movie.type == "series" || movie.type == "drama" -> "tvshows"
            cleanUrl.contains("/animes/") || movie.type == "anime" -> "animes"
            else -> "movies"
        }

        val baseMovieTitle = stripEpisodeSuffix(movie.title)
        var postId = movie.postId ?: -1
        var seasonsCount = 1
        var overview: String? = movie.overview
        var tagline: String? = null
        var runtime: String? = movie.runtime
        var releaseDate: String? = null
        var backdropUrl: String? = movie.backdrop
        var logoUrl: String? = movie.logo
        var trailerId: String? = null
        var galleryList: List<String> = emptyList()
        var ratingVal: String? = movie.rating
        var foundTitle: String = baseMovieTitle
        var genresList: List<String> = movie.genres

        // Fast lookup from titleSlugCache
        val cachedSlugPair = titleSlugCache[cleanTitleForMatch(baseMovieTitle)]
        if (cachedSlugPair != null) {
            if (postId <= 0) postId = cachedSlugPair.first
            if (slug.isNullOrBlank()) slug = cachedSlugPair.second
        }

        // Query LaMovie Single Item API for real full synopsis, backdrop, logo, runtime, gallery
        try {
            if (slug.isNullOrBlank()) {
                val qRaw = baseMovieTitle.split("(")[0].trim()
                val qClean = cleanTitleForMatch(qRaw)
                val alias = titleAliases[qClean]
                val queriesToTry = if (alias != null) listOf(qRaw, alias) else listOf(qRaw)

                for (q in queriesToTry) {
                    val encoded = java.net.URLEncoder.encode(q, "UTF-8").replace("+", "%20")
                    val searchUrl = "https://lamovie.org/wp-api/v1/search?q=$encoded&postType=any&postsPerPage=24"
                    val sReq = okhttp3.Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://lamovie.org/")
                        .build()
                    val sResp = ApiClient.okHttpClient.newCall(sReq).execute()
                    if (sResp.isSuccessful) {
                        val sBody = sResp.body?.string() ?: ""
                        val sJson = org.json.JSONObject(sBody)
                        val sPosts = sJson.optJSONObject("data")?.optJSONArray("posts")
                        if (sPosts != null && sPosts.length() > 0) {
                            val p = findBestMatchingPost(sPosts, baseMovieTitle, expectedType = postType)
                            if (p != null) {
                                postId = p.optInt("_id", postId)
                                slug = p.optString("slug", "").ifBlank { null }
                                postType = p.optString("type", postType)
                                break
                            }
                        }
                    }
                }
            }

            if (!slug.isNullOrBlank()) {
                val singleUrl = "https://lamovie.org/wp-api/v1/single/$postType?slug=$slug&postType=$postType"
                val req = okhttp3.Request.Builder()
                    .url(singleUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://lamovie.org/")
                    .build()
                val resp = ApiClient.okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val singleJson = org.json.JSONObject(body)
                    val dataObj = singleJson.optJSONObject("data")
                    if (dataObj != null) {
                        postId = dataObj.optInt("_id", postId)
                        val apiTitle = dataObj.optString("title", foundTitle)
                        foundTitle = if (baseMovieTitle.isNotBlank()) baseMovieTitle else apiTitle
                        val ov = dataObj.optString("overview", "").trim()
                        if (ov.isNotBlank()) overview = ov
                        val tg = dataObj.optString("tagline", "").trim()
                        if (tg.isNotBlank()) tagline = tg
                        val rt = dataObj.optString("runtime", "").trim()
                        if (rt.isNotBlank()) runtime = rt
                        val rd = dataObj.optString("release_date", "").trim()
                        if (rd.isNotBlank()) releaseDate = rd
                        val tr = dataObj.optString("trailer", "").trim()
                        if (tr.isNotBlank()) trailerId = tr
                        val rtVal = dataObj.optString("rating", "").trim()
                        if (rtVal.isNotBlank()) ratingVal = rtVal

                        val rawSeasons = dataObj.optJSONArray("seasons")
                        val latestEpSeason = dataObj.optJSONObject("latest_episode")?.optInt("season", 0) ?: 0
                        val numSeasons = dataObj.optInt("number_of_seasons", dataObj.optInt("seasons_count", latestEpSeason))
                        if (rawSeasons != null && rawSeasons.length() > 0) {
                            seasonsCount = rawSeasons.length().coerceAtLeast(1)
                        } else if (numSeasons > 0) {
                            seasonsCount = numSeasons
                        }

                        val galStr = dataObj.optString("gallery", "")
                        if (galStr.isNotBlank()) {
                            val rawPaths = galStr.split("\n", "\r\n")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            galleryList = rawPaths.mapIndexed { idx, path ->
                                if (path.startsWith("http")) path
                                else "https://image.tmdb.org/t/p/w1280$path"
                            }
                            if (galleryList.isNotEmpty()) {
                                backdropUrl = galleryList.first()
                            }
                        }

                        val images = dataObj.optJSONObject("images")
                        val bdRel = images?.optString("backdrop", "") ?: ""
                        if (backdropUrl.isNullOrBlank() && bdRel.isNotBlank()) {
                            backdropUrl = if (bdRel.startsWith("http")) bdRel else "https://lamovie.org/wp-content/uploads$bdRel"
                        }
                        val lgRel = images?.optString("logo", "") ?: ""
                        if (lgRel.isNotBlank()) {
                            logoUrl = if (lgRel.startsWith("http")) lgRel else "https://lamovie.org/wp-content/uploads$lgRel"
                        }

                        val gArray = dataObj.optJSONArray("genres")
                        if (gArray != null && gArray.length() > 0) {
                            val parsedGenres = mutableListOf<String>()
                            for (gi in 0 until gArray.length()) {
                                val gId = gArray.optInt(gi, -1)
                                val gName = laMovieGenresMap[gId]
                                if (gName != null && !parsedGenres.contains(gName)) {
                                    parsedGenres.add(gName)
                                }
                            }
                            if (parsedGenres.isNotEmpty()) {
                                genresList = parsedGenres
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val isSeriesContent = (postType == "tvshows" || postType == "animes" ||
            movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
            cleanUrl.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") || movie.url.contains("/novelas/"))

        val finalBackdrop = (backdropUrl ?: movie.backdrop ?: movie.image)
            ?.replace("/t/p/original/", "/t/p/w1280/") ?: ""
        val finalGenres = if (genresList.isNotEmpty()) genresList else when (postType) {
            "tvshows" -> listOf("Serie", "Drama", "Streaming")
            "animes" -> listOf("Anime", "Animación", "Acción")
            else -> listOf("Acción", "Ciencia Ficción", "Cine Internacional")
        }

        val finalOverview = overview ?: movie.overview
            ?: "${foundTitle} está disponible en calidad de streaming ultra alta definición con audio latino e idioma original en TV Pro."

        // Build strictly isolated Similar Movies / Series / Anime recommendations
        val similarContentType = when {
            postType == "animes" || movie.type == "anime" || movie.url.contains("/animes/") -> "animes"
            postType == "tvshows" || movie.type == "series" || movie.url.contains("/series/") || movie.url.contains("/tvshows/") -> "series"
            else -> "movies"
        }
        val localFallbackPool = when (similarContentType) {
            "animes" -> localAnimeCatalog
            "series" -> (localSeriesCatalog + localDramaCatalog)
            else -> localCatalog
        }

        // Run episodes, streams resolution, and similar movies in parallel to eliminate metadata delay!
        val (initialEpisodes, candidateStreams, similarItems) = coroutineScope {
            val episodesDeferred = async {
                if (isSeriesContent) {
                    withTimeoutOrNull(4500L) {
                        fetchEpisodesForSeason(
                            postId = postId,
                            season = 1,
                            fallbackTitle = foundTitle,
                            fallbackBackdrop = finalBackdrop,
                            fallbackGallery = galleryList
                        )
                    } ?: emptyList()
                } else emptyList()
            }

            val streamsDeferred = async {
                val candidateList = mutableListOf<StreamItem>()
                if (!isSeriesContent) {
                    val directPostStreams = if (postId > 0) {
                        withTimeoutOrNull(5000L) {
                            resolveEpisodeStreams(
                                episodeId = postId,
                                seriesTitle = foundTitle,
                                fallbackStreams = emptyList(),
                                seasonNumber = 1,
                                episodeNumber = 1,
                                seriesPostId = postId
                            )
                        } ?: emptyList()
                    } else emptyList()

                    val laMovieStreams = if (directPostStreams.isNotEmpty()) {
                        directPostStreams
                    } else {
                        withTimeoutOrNull(4000L) {
                            resolveLaMovieStreams(movie.title)
                        } ?: emptyList()
                    }

                    for (stream in laMovieStreams) {
                        if (candidateList.none { it.url == stream.url }) {
                            candidateList.add(stream)
                        }
                    }
                }

                if (candidateList.isEmpty()) {
                    val curated = getStreamsForMovie(movie.url)
                    val extractedCurated = mutableListOf<StreamItem>()
                    for (item in curated) {
                        if (item.url.contains("vimeos")) {
                            val direct = extractVimeosDirectStreams(item.url, "Latino", "Full HD")
                            if (direct.isNotEmpty()) extractedCurated.addAll(direct) else extractedCurated.add(item)
                        } else if (item.url.contains("voe")) {
                            val direct = extractVoeDirectStreams(item.url, "Latino", "Full HD")
                            if (direct.isNotEmpty()) extractedCurated.addAll(direct) else extractedCurated.add(item)
                        } else {
                            extractedCurated.add(item)
                        }
                    }
                    val curatedSubs = extractedCurated.flatMap { it.safeSubtitles }.distinctBy { it.url }
                    val enrichedCurated = if (curatedSubs.isNotEmpty()) {
                        extractedCurated.map { s ->
                            val merged = (s.safeSubtitles + curatedSubs).distinctBy { it.url }
                            s.copy(subtitle = 1, subtitleTracks = merged)
                        }
                    } else {
                        extractedCurated
                    }
                    val preStreams = filterWorkingStreams(enrichedCurated)
                    candidateList.addAll(preStreams)
                }
                candidateList
            }

            val similarDeferred = async {
                val liveSameGenreSimilar = try {
                    val primaryGenreId = finalGenres.firstNotNullOfOrNull { gName ->
                        laMovieGenresMap.entries.firstOrNull { it.value.equals(gName, ignoreCase = true) }?.key
                    }
                    val res = withTimeoutOrNull(3500L) {
                        fetchCategoryListing(
                            categoryType = similarContentType,
                            page = 1,
                            postsPerPage = 12,
                            genreIds = listOfNotNull(primaryGenreId),
                            orderBy = "popular"
                        )
                    }
                    res?.items?.filter { !it.title.equals(foundTitle, ignoreCase = true) && it.url != movie.url } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                val matchedLocal = localFallbackPool.filter { it.url != movie.url && it.genres.any { g -> finalGenres.any { fg -> fg.equals(g, ignoreCase = true) } } }
                val fallbackItems = matchedLocal.ifEmpty { localFallbackPool.filter { it.url != movie.url } }
                val similarItemsList = if (liveSameGenreSimilar.isNotEmpty()) {
                    if (liveSameGenreSimilar.size >= 8) {
                        liveSameGenreSimilar
                    } else {
                        (liveSameGenreSimilar + matchedLocal).distinctBy { it.url }
                    }
                } else {
                    fallbackItems
                }
                similarItemsList
                    .filter { !it.title.equals(foundTitle, ignoreCase = true) && it.url != movie.url }
                    .distinctBy { it.url }
                    .take(12)
            }

            Triple(episodesDeferred.await(), streamsDeferred.await(), similarDeferred.await())
        }

        if (isSeriesContent && postId > 0) {
            val detectedFromEpisodes = seasonsCountCache[postId] ?: 0
            val maxEpSeason = initialEpisodes.maxOfOrNull { it.seasonNumber } ?: 1
            seasonsCount = maxOf(seasonsCount, detectedFromEpisodes, maxEpSeason)
        }

        val response = MovieDetailResponse(
            status = "success",
            postId = if (postId > 0) postId.toString() else null,
            title = foundTitle,
            tagline = tagline,
            overview = finalOverview,
            rating = ratingVal ?: movie.rating ?: "8.5",
            tmdbRating = ratingVal ?: movie.rating ?: "8.5",
            description = finalOverview,
            poster = movie.image,
            backdrop = finalBackdrop,
            logo = logoUrl ?: movie.logo,
            runtime = runtime ?: movie.runtime,
            releaseDate = releaseDate ?: movie.year,
            trailer = trailerId,
            genres = finalGenres,
            gallery = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(backdropUrl, movie.backdrop, movie.image).distinct(),
            streams = candidateStreams,
            streamsCount = candidateStreams.size,
            isSeries = isSeriesContent,
            seasonsCount = seasonsCount.coerceAtLeast(1),
            episodes = initialEpisodes,
            similarMovies = similarItems
        )
        cacheDetail(movie.url, foundTitle, response)
        cacheDetail(cleanUrl, baseMovieTitle, response)
        response
    }

    suspend fun extractMovie(movieUrl: String, forceRefresh: Boolean = false): MovieDetailResponse = withContext(Dispatchers.IO) {
        val allCurated = (localCatalog + localSeriesCatalog + localAnimeCatalog)
        val found = allCurated.find { it.url == movieUrl || movieUrl.contains(it.title) }
            ?: MovieItem(
                title = movieUrl.trimEnd('/').substringAfterLast("/").replace("-", " "),
                url = movieUrl,
                image = "",
                type = if (movieUrl.contains("/series/") || movieUrl.contains("/tvshows/") || movieUrl.contains("/animes/")) "series" else "movie"
            )
        extractMovie(found, forceRefresh)
    }

    fun normalizeForSearch(text: String): String {
        val nfd = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+")
            .replace(nfd, "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .lowercase()
            .trim()
    }

    data class TitleAliasGroup(
        val english: List<String>,
        val spanish: List<String>
    )

    private val titleAliasGroups: List<TitleAliasGroup> = listOf(
        TitleAliasGroup(listOf("fast and furious", "the fast and the furious", "fast & furious", "furious"), listOf("rapidos y furiosos", "a todo gas")),
        TitleAliasGroup(listOf("home alone"), listOf("mi pobre angelito", "solo en casa")),
        TitleAliasGroup(listOf("inside out"), listOf("intensamente", "del reves")),
        TitleAliasGroup(listOf("die hard"), listOf("duro de matar", "la jungla de cristal")),
        TitleAliasGroup(listOf("the simpsons", "simpsons"), listOf("los simpson", "los simpsons")),
        TitleAliasGroup(listOf("the dark knight", "dark knight"), listOf("el caballero de la noche", "el caballero oscuro")),
        TitleAliasGroup(listOf("game of thrones", "got"), listOf("juego de tronos")),
        TitleAliasGroup(listOf("squid game"), listOf("el juego del calamar")),
        TitleAliasGroup(listOf("attack on titan", "shingeki no kyojin", "snk"), listOf("ataque a los titanes", "ataque de los titanes")),
        TitleAliasGroup(listOf("demon slayer", "kimetsu no yaiba"), listOf("guardianes de la noche")),
        TitleAliasGroup(listOf("the lord of the rings", "lord of the rings", "lotr"), listOf("el senor de los anillos", "el señor de los anillos")),
        TitleAliasGroup(listOf("star wars"), listOf("la guerra de las galaxias")),
        TitleAliasGroup(listOf("pirates of the caribbean"), listOf("piratas del caribe")),
        TitleAliasGroup(listOf("the hangover"), listOf("que paso ayer", "resacon en las vegas")),
        TitleAliasGroup(listOf("back to the future"), listOf("volver al futuro", "regreso al futuro")),
        TitleAliasGroup(listOf("pulp fiction"), listOf("tiempos violentos")),
        TitleAliasGroup(listOf("money heist"), listOf("la casa de papel")),
        TitleAliasGroup(listOf("the lion king", "lion king"), listOf("el rey leon")),
        TitleAliasGroup(listOf("finding nemo"), listOf("buscando a nemo")),
        TitleAliasGroup(listOf("the godfather", "godfather"), listOf("el padrino")),
        TitleAliasGroup(listOf("ghostbusters"), listOf("los cazafantasmas")),
        TitleAliasGroup(listOf("men in black", "mib"), listOf("hombres de negro")),
        TitleAliasGroup(listOf("night at the museum"), listOf("una noche en el museo")),
        TitleAliasGroup(listOf("the walking dead", "twd"), listOf("los muertos vivientes")),
        TitleAliasGroup(listOf("the shawshank redemption", "shawshank redemption"), listOf("sueno de fuga", "cadena perpetua")),
        TitleAliasGroup(listOf("fight club"), listOf("el club de la pelea", "el club de la lucha")),
        TitleAliasGroup(listOf("the shining"), listOf("el resplandor")),
        TitleAliasGroup(listOf("the avengers", "avengers"), listOf("los vengadores")),
        TitleAliasGroup(listOf("jurassic park"), listOf("parque jurasico")),
        TitleAliasGroup(listOf("jurassic world"), listOf("mundo jurasico")),
        TitleAliasGroup(listOf("ice age"), listOf("la era de hielo", "era de hielo")),
        TitleAliasGroup(listOf("despicable me"), listOf("mi villano favorito")),
        TitleAliasGroup(listOf("how to train your dragon"), listOf("como entrenar a tu dragon")),
        TitleAliasGroup(listOf("monsters inc", "monsters, inc."), listOf("monsters sa", "monstruos sa")),
        TitleAliasGroup(listOf("the incredibles", "incredibles"), listOf("los increibles")),
        TitleAliasGroup(listOf("finding dory"), listOf("buscando a dory")),
        TitleAliasGroup(listOf("tangled"), listOf("enredados")),
        TitleAliasGroup(listOf("frozen"), listOf("una aventura congelada")),
        TitleAliasGroup(listOf("moana"), listOf("vaiana")),
        TitleAliasGroup(listOf("wreck it ralph", "wreck-it ralph"), listOf("ralph el demoledor")),
        TitleAliasGroup(listOf("big hero 6"), listOf("grandes heroes")),
        TitleAliasGroup(listOf("scary movie"), listOf("una pelicula de miedo")),
        TitleAliasGroup(listOf("the parent trap"), listOf("juego de gemelas")),
        TitleAliasGroup(listOf("groundhog day"), listOf("el dia de la marmota", "hechizo del tiempo")),
        TitleAliasGroup(listOf("saving private ryan"), listOf("rescatando al soldado ryan", "salvar al soldado ryan")),
        TitleAliasGroup(listOf("goodfellas"), listOf("buenos muchachos")),
        TitleAliasGroup(listOf("the silence of the lambs"), listOf("el silencio de los inocentes", "el silencio de los corderos")),
        TitleAliasGroup(listOf("spirited away", "sen to chihiro"), listOf("el viaje de chihiro")),
        TitleAliasGroup(listOf("my neighbor totoro", "tonari no totoro"), listOf("mi vecino totoro")),
        TitleAliasGroup(listOf("princess mononoke", "mononoke hime"), listOf("la princesa mononoke")),
        TitleAliasGroup(listOf("weathering with you", "tenki no ko"), listOf("el tiempo contigo")),
        TitleAliasGroup(listOf("chainsaw man"), listOf("el hombre motosierra")),
        TitleAliasGroup(listOf("death note"), listOf("el cuaderno de la muerte")),
        TitleAliasGroup(listOf("fullmetal alchemist", "fma"), listOf("el alquimista de acero")),
        TitleAliasGroup(listOf("my hero academia", "mha", "boku no hero academia", "bnha"), listOf("mi academia de heroes")),
        TitleAliasGroup(listOf("house of the dragon", "hotd"), listOf("la casa del dragon")),
        TitleAliasGroup(listOf("band of brothers"), listOf("hermanos de sangre")),
        TitleAliasGroup(listOf("lost"), listOf("perdidos")),
        TitleAliasGroup(listOf("how i met your mother", "himym"), listOf("como conoci a tu madre")),
        TitleAliasGroup(listOf("the big bang theory", "tbbt"), listOf("la teoria del big bang")),
        TitleAliasGroup(listOf("supernatural"), listOf("sobrenatural")),
        TitleAliasGroup(listOf("greys anatomy", "grey's anatomy"), listOf("anatomia de grey", "anatomia segun grey")),
        TitleAliasGroup(listOf("dr house", "house md"), listOf("doctor house")),
        TitleAliasGroup(listOf("vikings"), listOf("vikingos")),
        TitleAliasGroup(listOf("parasite"), listOf("parasitos")),
        TitleAliasGroup(listOf("everything everywhere all at once"), listOf("todo en todas partes al mismo tiempo")),
        TitleAliasGroup(listOf("whiplash"), listOf("musica y obsesion")),
        TitleAliasGroup(listOf("la la land"), listOf("una historia de amor", "ciudad de las estrellas")),
        TitleAliasGroup(listOf("django unchained"), listOf("django sin cadenas")),
        TitleAliasGroup(listOf("inglourious basterds"), listOf("bastardos sin gloria", "malditos bastardos")),
        TitleAliasGroup(listOf("reservoir dogs"), listOf("perros de la calle")),
        TitleAliasGroup(listOf("the truman show"), listOf("el show de truman")),
        TitleAliasGroup(listOf("eternal sunshine of the spotless mind"), listOf("eterno resplandor de una mente sin recuerdos", "olvidate de mi")),
        TitleAliasGroup(listOf("the wolf of wall street"), listOf("el lobo de wall street")),
        TitleAliasGroup(listOf("shutter island"), listOf("la isla siniestra")),
        TitleAliasGroup(listOf("catch me if you can"), listOf("atrapame si puedes")),
        TitleAliasGroup(listOf("the departed"), listOf("los infiltrados", "infiltrados")),
        TitleAliasGroup(listOf("blood diamond"), listOf("diamante de sangre")),
        TitleAliasGroup(listOf("cast away"), listOf("naufrago")),
        TitleAliasGroup(listOf("the terminal"), listOf("la terminal")),
        TitleAliasGroup(listOf("the green mile"), listOf("milagros inesperados", "la milla verde")),
        TitleAliasGroup(listOf("twilight"), listOf("crepusculo")),
        TitleAliasGroup(listOf("the hunger games", "hunger games"), listOf("los juegos del hambre")),
        TitleAliasGroup(listOf("maze runner", "the maze runner"), listOf("correr o morir")),
        TitleAliasGroup(listOf("fantastic beasts"), listOf("animales fantasticos")),
        TitleAliasGroup(listOf("dune"), listOf("duna")),
        TitleAliasGroup(listOf("john wick"), listOf("otro dia para matar")),
        TitleAliasGroup(listOf("taken"), listOf("busqueda implacable", "venganza")),
        TitleAliasGroup(listOf("mission impossible", "mission: impossible"), listOf("mision imposible")),
        TitleAliasGroup(listOf("the bourne identity", "bourne"), listOf("identidad desconocida")),
        TitleAliasGroup(listOf("kingsman"), listOf("el servicio secreto")),
        TitleAliasGroup(listOf("now you see me"), listOf("los ilusionistas", "nada es lo que parece")),
        TitleAliasGroup(listOf("oceans eleven", "ocean's eleven"), listOf("la gran estafa")),
        TitleAliasGroup(listOf("the prestige"), listOf("el gran truco", "el prestigio")),
        TitleAliasGroup(listOf("memento"), listOf("amnesia")),
        TitleAliasGroup(listOf("dunkirk"), listOf("dunkerque")),
        TitleAliasGroup(listOf("gravity"), listOf("gravedad")),
        TitleAliasGroup(listOf("the martian"), listOf("mision rescate", "marte")),
        TitleAliasGroup(listOf("arrival"), listOf("la llegada")),
        TitleAliasGroup(listOf("edge of tomorrow"), listOf("al filo del manana", "al filo del mañana")),
        TitleAliasGroup(listOf("source code"), listOf("8 minutos antes de morir", "codigo fuente")),
        TitleAliasGroup(listOf("looper"), listOf("asesinos del futuro")),
        TitleAliasGroup(listOf("limitless"), listOf("sin limites")),
        TitleAliasGroup(listOf("i am legend"), listOf("soy leyenda")),
        TitleAliasGroup(listOf("world war z"), listOf("guerra mundial z")),
        TitleAliasGroup(listOf("28 days later"), listOf("exterminio", "28 dias despues")),
        TitleAliasGroup(listOf("train to busan"), listOf("estacion zombie", "tren a busan")),
        TitleAliasGroup(listOf("a quiet place"), listOf("un lugar en silencio", "un lugar tranquilo")),
        TitleAliasGroup(listOf("bird box"), listOf("a ciegas")),
        TitleAliasGroup(listOf("the conjuring"), listOf("el conjuro", "expediente warren")),
        TitleAliasGroup(listOf("insidious"), listOf("la noche del demonio")),
        TitleAliasGroup(listOf("sinister"), listOf("siniestro")),
        TitleAliasGroup(listOf("hereditary"), listOf("el legado del diablo")),
        TitleAliasGroup(listOf("get out"), listOf("huye", "dejame salir")),
        TitleAliasGroup(listOf("us"), listOf("nosotros")),
        TitleAliasGroup(listOf("nope"), listOf("nop")),
        TitleAliasGroup(listOf("the purge"), listOf("la noche de la expiacion", "la purga")),
        TitleAliasGroup(listOf("the ring"), listOf("el aro", "la senal")),
        TitleAliasGroup(listOf("the grudge"), listOf("la maldicion", "el grito")),
        TitleAliasGroup(listOf("it"), listOf("eso")),
        TitleAliasGroup(listOf("paranormal activity"), listOf("actividad paranormal")),
        TitleAliasGroup(listOf("final destination"), listOf("destino final")),
        TitleAliasGroup(listOf("evil dead"), listOf("posesion infernal", "el despertar del diablo")),
        TitleAliasGroup(listOf("scream"), listOf("grita antes de morir")),
        TitleAliasGroup(listOf("friday the 13th"), listOf("viernes 13")),
        TitleAliasGroup(listOf("a nightmare on elm street"), listOf("pesadilla en la calle del infierno", "pesadilla en elm street")),
        TitleAliasGroup(listOf("childs play", "child's play"), listOf("el muneco diabolico", "el muñeco diabolico")),
        TitleAliasGroup(listOf("edward scissorhands"), listOf("el joven manos de tijera", "eduardo manostijeras")),
        TitleAliasGroup(listOf("the nightmare before christmas"), listOf("el extrano mundo de jack", "el extraño mundo de jack", "pesadilla antes de navidad")),
        TitleAliasGroup(listOf("corpse bride"), listOf("el cadaver de la novia")),
        TitleAliasGroup(listOf("coraline"), listOf("coraline y la puerta secreta")),
        TitleAliasGroup(listOf("charlie and the chocolate factory"), listOf("charlie y la fabrica de chocolate")),
        TitleAliasGroup(listOf("alice in wonderland"), listOf("alicia en el pais de las maravillas")),
        TitleAliasGroup(listOf("maleficent"), listOf("malefica")),
        TitleAliasGroup(listOf("cinderella"), listOf("la cenicienta")),
        TitleAliasGroup(listOf("beauty and the beast"), listOf("la bella y la bestia")),
        TitleAliasGroup(listOf("aladdin"), listOf("aladino")),
        TitleAliasGroup(listOf("the little mermaid"), listOf("la sirenita")),
        TitleAliasGroup(listOf("pinocchio"), listOf("pinocho")),
        TitleAliasGroup(listOf("the hunchback of notre dame"), listOf("el jorobado de notre dame")),
        TitleAliasGroup(listOf("sleeping beauty"), listOf("la bella durmiente")),
        TitleAliasGroup(listOf("snow white"), listOf("blanca nieves", "blancanieves")),
        TitleAliasGroup(listOf("sing"), listOf("sing ven y canta", "canta")),
        TitleAliasGroup(listOf("the secret life of pets"), listOf("la vida secreta de tus mascotas", "mascotas")),
        TitleAliasGroup(listOf("megamind"), listOf("megamente")),
        TitleAliasGroup(listOf("monsters vs aliens"), listOf("monstruos vs aliens")),
        TitleAliasGroup(listOf("the croods"), listOf("los croods")),
        TitleAliasGroup(listOf("the boss baby", "boss baby"), listOf("un jefe en panales", "el bebe jefazo")),
        TitleAliasGroup(listOf("the bad guys"), listOf("los tipos malos")),
        TitleAliasGroup(listOf("puss in boots"), listOf("el gato con botas")),
        TitleAliasGroup(listOf("hotel transylvania"), listOf("hotel transilvania")),
        TitleAliasGroup(listOf("cloudy with a chance of meatballs"), listOf("lluvia de hamburguesas")),
        TitleAliasGroup(listOf("into the spider-verse"), listOf("un nuevo universo")),
        TitleAliasGroup(listOf("across the spider-verse"), listOf("a traves del spider-verso")),
        TitleAliasGroup(listOf("fantastic four"), listOf("los cuatro fantasticos")),
        TitleAliasGroup(listOf("guardians of the galaxy"), listOf("guardianes de la galaxia")),
        TitleAliasGroup(listOf("captain america"), listOf("capitan america")),
        TitleAliasGroup(listOf("black panther"), listOf("pantera negra")),
        TitleAliasGroup(listOf("doctor strange"), listOf("dr strange")),
        TitleAliasGroup(listOf("ant-man", "ant man"), listOf("el hombre hormiga")),
        TitleAliasGroup(listOf("spider man", "spiderman"), listOf("el hombre arana", "el hombre araña")),
        TitleAliasGroup(listOf("iron man"), listOf("el hombre de hierro")),
        TitleAliasGroup(listOf("saw"), listOf("el juego del miedo")),
        TitleAliasGroup(listOf("interstellar"), listOf("interestelar")),
        TitleAliasGroup(listOf("wonder woman"), listOf("mujer maravilla")),
        TitleAliasGroup(listOf("justice league"), listOf("la liga de la justicia")),
        TitleAliasGroup(listOf("the suicide squad", "suicide squad"), listOf("el escuadron suicida")),
        TitleAliasGroup(listOf("joker"), listOf("guason", "guasón"))
    )

    fun getSearchTermsForQuery(query: String): List<String> {
        val clean = normalizeForSearch(query)
        val terms = mutableListOf(query.trim())
        if (clean.isBlank()) return terms

        for (group in titleAliasGroups) {
            val engNorm = group.english.map { normalizeForSearch(it) }
            val espNorm = group.spanish.map { normalizeForSearch(it) }

            val isEngMatch = engNorm.any { clean == it || (clean.length >= 3 && (it.startsWith(clean) || clean.startsWith(it))) }
            val isEspMatch = espNorm.any { clean == it || (clean.length >= 3 && (it.startsWith(clean) || clean.startsWith(it))) }

            val candidates = mutableListOf<String>()
            if (isEngMatch) {
                candidates.addAll(group.spanish)
                candidates.addAll(group.english)
            } else if (isEspMatch) {
                candidates.addAll(group.spanish)
                candidates.addAll(group.english)
            }

            for (c in candidates) {
                if (normalizeForSearch(c) != clean && !terms.contains(c)) {
                    terms.add(c)
                }
            }
        }
        return terms.take(3)
    }

    private suspend fun fetchRemoteSearchResults(term: String): List<MovieItem> = withTimeoutOrNull(4500L) {
        try {
            val encoded = java.net.URLEncoder.encode(term.trim(), "UTF-8").replace("+", "%20")
            val searchUrl = "https://lamovie.org/wp-api/v1/search?q=$encoded&postType=any&postsPerPage=24"
            val searchReq = okhttp3.Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://lamovie.org/")
                .build()

            val searchResp = ApiClient.okHttpClient.newCall(searchReq).execute()
            if (!searchResp.isSuccessful) return@withTimeoutOrNull null

            val searchBody = searchResp.body?.string() ?: return@withTimeoutOrNull null
            val searchJson = org.json.JSONObject(searchBody)
            val posts = searchJson.optJSONObject("data")?.optJSONArray("posts") ?: return@withTimeoutOrNull null
            val list = mutableListOf<MovieItem>()
            for (i in 0 until posts.length()) {
                val p = posts.getJSONObject(i)
                val pId = p.optInt("_id", -1).takeIf { it > 0 }
                val title = p.optString("title", "")
                val slug = p.optString("slug", "").ifBlank { null }
                val pType = p.optString("type", "movies")
                val images = p.optJSONObject("images")
                val posterRel = images?.optString("poster", "") ?: ""
                val posterUrl = if (posterRel.isNotBlank() && !posterRel.equals("null", ignoreCase = true)) {
                    if (posterRel.startsWith("http")) posterRel else "https://lamovie.org/wp-content/uploads$posterRel"
                } else ""
                val galleryFirst = p.optString("gallery", "")
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val backdropRel = images?.optString("backdrop", "") ?: ""
                val backdropUrl = when {
                    !galleryFirst.isNullOrBlank() ->
                        if (galleryFirst.startsWith("http")) galleryFirst else "https://image.tmdb.org/t/p/w1280$galleryFirst"
                    backdropRel.isNotBlank() && !backdropRel.equals("null", ignoreCase = true) ->
                        if (backdropRel.startsWith("http")) backdropRel else "https://lamovie.org/wp-content/uploads$backdropRel"
                    else -> null
                }
                val logoRel = images?.optString("logo", "") ?: ""
                val logoUrl = if (logoRel.isNotBlank() && !logoRel.equals("null", ignoreCase = true)) {
                    if (logoRel.startsWith("http")) logoRel else "https://lamovie.org/wp-content/uploads$logoRel"
                } else null
                val overview = p.optString("overview", "").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val runtimeStr = p.optString("runtime", "").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                val ratingNum = p.optDouble("rating", 7.5)
                val releaseDate = p.optString("release_date", "")
                val yearMatch = Regex("""\((\d{4})\)""").find(title)?.groupValues?.get(1)
                    ?: if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2024"

                val itemPath = when (pType) {
                    "tvshows" -> "series"
                    "animes" -> "animes"
                    else -> "peliculas"
                }
                val itemType = when (pType) {
                    "tvshows" -> "series"
                    "animes" -> "anime"
                    else -> "normal"
                }

                val parsedGenres = mutableListOf<String>()
                val gArray = p.optJSONArray("genres")
                if (gArray != null) {
                    for (gi in 0 until gArray.length()) {
                        val gId = gArray.optInt(gi, -1)
                        val gName = laMovieGenresMap[gId]
                        if (gName != null && !parsedGenres.contains(gName)) {
                            parsedGenres.add(gName)
                        }
                    }
                }

                if (title.isNotBlank()) {
                    if (pId != null && slug != null) {
                        titleSlugCache[cleanTitleForMatch(title)] = Pair(pId, slug)
                    }
                    list.add(
                        MovieItem(
                            title = title,
                            url = "https://lamovie.org/$itemPath/${slug ?: ""}",
                            image = posterUrl,
                            backdrop = backdropUrl,
                            logo = logoUrl,
                            type = itemType,
                            rating = String.format(java.util.Locale.US, "%.1f", ratingNum),
                            year = yearMatch,
                            quality = "Full HD",
                            languages = listOf("Latino"),
                            overview = overview,
                            runtime = runtimeStr,
                            postId = pId,
                            slug = slug,
                            genres = parsedGenres
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            null
        }
    } ?: emptyList()

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val cleanQueryNorm = normalizeForSearch(query)
        val searchTerms = getSearchTermsForQuery(query)
        val allSearchNorms = searchTerms.map { normalizeForSearch(it) }

        val allCurated = (localCatalog + localSeriesCatalog + localAnimeCatalog + localDramaCatalog)
        val localMatches = allCurated.filter { item ->
            val titleNorm = normalizeForSearch(item.title)
            allSearchNorms.any { termNorm ->
                titleNorm.contains(termNorm) ||
                item.genres.any { g -> normalizeForSearch(g).contains(termNorm) }
            } || item.year?.contains(query.trim()) == true
        }

        // Query LaMovie search endpoint for live results for each resolved search term concurrently
        val remoteMatches = coroutineScope {
            searchTerms.map { term ->
                async {
                    fetchRemoteSearchResults(term)
                }
            }.awaitAll()
        }.flatten()

        val combined = (remoteMatches + localMatches).distinctBy {
            if (it.postId != null && it.postId > 0) "id_${it.postId}"
            else if (!it.slug.isNullOrBlank()) "slug_${it.slug}"
            else it.title.replace(Regex("""\s*\(\d{4}\)"""), "").lowercase().trim()
        }

        combined.sortedWith(
            compareByDescending<MovieItem> { item ->
                val titleNorm = normalizeForSearch(item.title)
                when {
                    titleNorm == cleanQueryNorm -> 100
                    titleNorm.startsWith(cleanQueryNorm) -> 90
                    titleNorm.contains(cleanQueryNorm) -> 80
                    allSearchNorms.any { it.isNotBlank() && titleNorm == it } -> 70
                    allSearchNorms.any { it.isNotBlank() && titleNorm.startsWith(it) } -> 60
                    allSearchNorms.any { it.isNotBlank() && titleNorm.contains(it) } -> 50
                    else -> 10
                }
            }.thenByDescending {
                it.year?.toIntOrNull() ?: 0
            }
        )
    }
}
