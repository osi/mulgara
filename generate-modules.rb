require "fileutils"

  # Dir["src/jar/*"].each do |dir|
  #   puts "<module>#{dir}</module>"
  # end

def move_tests
  Dir.chdir("src/jar") do
    Dir["*"].each do |dir|
      Dir.chdir(dir) do
        puts "processing #{dir}"
        
        Dir["**/*Test.java"].each do |test|
          moved = test.gsub(/main\/java/, "test/java")
          FileUtils.mkdir_p moved.split("/")[0..-2].join('/')
          `/usr/local/bin/git mv #{test} #{moved}`
        end

        # if File.exists?("java")
        #   FileUtils.mkdir_p 'src/main'
        #   `/usr/local/bin/git mv java src/main/`
        # end

      end
    end
  end
end
move_tests
