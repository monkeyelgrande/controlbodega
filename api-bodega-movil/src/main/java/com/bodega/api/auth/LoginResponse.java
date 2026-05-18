package com.bodega.api.auth;

/**
 * Respuesta del login. La app guarda el {@code token} y lo envia en cada
 * peticion siguiente (cabecera Authorization: Bearer &lt;token&gt;).
 *
 * {@code idPerfil} permite a la app mostrar/ocultar opciones, pero la
 * validacion real de permisos siempre se hace en el servidor.
 */
public class LoginResponse {

    private String token;
    private int idUser;
    private String nombre;
    private String userName;
    private int idPerfil;
    private String perfil;
    private int idBodega;
    private String bodega;
    private boolean esAdmin;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(int idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public int getIdBodega() {
        return idBodega;
    }

    public void setIdBodega(int idBodega) {
        this.idBodega = idBodega;
    }

    public String getBodega() {
        return bodega;
    }

    public void setBodega(String bodega) {
        this.bodega = bodega;
    }

    public boolean isEsAdmin() {
        return esAdmin;
    }

    public void setEsAdmin(boolean esAdmin) {
        this.esAdmin = esAdmin;
    }
}
