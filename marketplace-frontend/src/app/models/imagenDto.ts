/** Imagen de un alojamiento (ImagenDTO): su id, la URL pública y si es la portada. */
export interface Imagen {
  
  idImagen: number;
  
  url: string;

  principal: boolean;
}
