describe 'Simple' do
  it 'should', :simple => true do
    expect(true).to eq true 
  end

  it 'has no $CLASSPATH entries', :counter => 'small' do
    expect($CLASSPATH.size).to eq 0
  end

  it 'has some loaded gems', :counter => 'big', :simple => false do
    expect(Gem.loaded_specs.size).to eq 10
  end

  it 'has some loaded gems', :simple => false do
    require 'rspec'
    expect(Gem.loaded_specs['rspec'].version.to_s).not_to eq '3.2.0'
  end
end
