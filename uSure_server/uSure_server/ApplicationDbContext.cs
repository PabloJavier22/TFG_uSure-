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
            modelBuilder.Entity<GrupoProducto>()
                .HasKey(gp => new { gp.IDGrupo, gp.IDProducto });

            modelBuilder.Entity<UsuarioGrupo>()
             .HasKey(ug => new { ug.UsuarioUID, ug.GrupoId });

            modelBuilder.Entity<UsuarioGrupo>()
                .HasOne(ug => ug.Usuario)
                .WithMany(u => u.UsuariosGrupos)
                .HasForeignKey(ug => ug.UsuarioUID);

            modelBuilder.Entity<UsuarioGrupo>()
                .HasOne(ug => ug.Grupo)
                .WithMany(g => g.Usuarios)
                .HasForeignKey(ug => ug.GrupoId);
        }
    }
}
