describe 'Simple' do
  it 'should' do
    expect(true).to eq true 
  end

  it 'has no $CLASSPATH entries' do
    expect($CLASSPATH.size).to eq 0
  end

  it 'has some loaded gems' do
    expect(Gem.loaded_specs.size).to eq 6
  end

  it 'has some loaded gems' do
    require 'rspec'
    expect(Gem.loaded_specs['rspec'].version.to_s).not_to eq '3.2.0'
  end
end
