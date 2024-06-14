using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using uSure_server.Controllers;
using Newtonsoft.Json;

namespace uSure_server
{
    public class ApplicationDbContext : DbContext
    {

      
        public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
            : base(options)
        {
        }

        public DbSet<Controllers.Usuario> Usuarios { get; set; }
        public DbSet<Controllers.Grupo> Grupos { get; set; }
        public DbSet<Controllers.Categoria> Categorias { get; set; }
        public DbSet<Controllers.Producto> Productos { get; set; }
        public DbSet<Controllers.GrupoProducto> Grupo_Producto { get; set; }
        public DbSet<UsuarioGrupo> UsuarioGrupo { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            // Configuring many-to-many relationship for UsuarioGrupo
            modelBuilder.Entity<UsuarioGrupo>()
                .HasKey(ug => new { ug.UsuarioUID, ug.GrupoId });

            modelBuilder.Entity<UsuarioGrupo>()
                .HasOne(ug => ug.Usuario)
                .WithMany(u => u.UsuarioGrupo)
                .HasForeignKey(ug => ug.UsuarioUID);

            modelBuilder.Entity<UsuarioGrupo>()
                .HasOne(ug => ug.Grupo)
                .WithMany(g => g.Usuarios)
                .HasForeignKey(ug => ug.GrupoId);

            // Configuring one-to-many relationship for Grupo-Categoria
            modelBuilder.Entity<Categoria>()
                .HasOne(c => c.Grupo)
                .WithMany(g => g.Categorias)
                .HasForeignKey(c => c.IDGrupo);

            // Configuring one-to-many relationship for Categoria-Producto
            modelBuilder.Entity<Producto>()
                .HasOne(p => p.Categoria)
                .WithMany(c => c.Productos)
                .HasForeignKey(p => p.IDCategoria);

            // Configuring many-to-many relationship for GrupoProducto
            modelBuilder.Entity<GrupoProducto>()
                .HasKey(gp => new { gp.IDGrupo, gp.IDProducto });

            modelBuilder.Entity<GrupoProducto>()
                .HasOne(gp => gp.Grupo)
                .WithMany(g => g.GrupoProductos)
                .HasForeignKey(gp => gp.IDGrupo);

            modelBuilder.Entity<GrupoProducto>()
                .HasOne(gp => gp.Producto)
                .WithMany(p => p.GrupoProductos)
                .HasForeignKey(gp => gp.IDProducto);
        }
    }
}
