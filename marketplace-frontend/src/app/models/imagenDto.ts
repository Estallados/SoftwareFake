/** Imagen de un alojamiento (ImagenDTO): su id, la URL pública y si es la portada. */
export interface Imagen {
  /** Identificador de la imagen. */
  idImagen: number;
  /** Dirección pública de la imagen. */
  url: string;
  /** Indica si es la portada del alojamiento. */
  principal: boolean;
}
