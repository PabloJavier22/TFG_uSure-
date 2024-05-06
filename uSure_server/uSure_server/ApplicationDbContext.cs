using Microsoft.EntityFrameworkCore;
using System.Collections.Generic;
using System.Text.RegularExpressions;
using uSure_server.Controllers;

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

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<GrupoProducto>()
                .HasKey(gp => new { gp.ID_Grupo, gp.ID_Producto });
        }
    }
}
